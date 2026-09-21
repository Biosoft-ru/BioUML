#!/usr/bin/env python3
# PYTHON_ARGCOMPLETE_OK
"""
run.py: Run conformance tests for WDL, grabbing the tests from the tests folder and expected values from
conformance.yaml
"""
import os
import json
import re

import sys
import hashlib
import argparse
import argcomplete
import threading
import timeit

from ruamel.yaml import YAML

from concurrent.futures import as_completed, ProcessPoolExecutor
from shutil import which
from uuid import uuid4

from WDL.Type import (
    Float as WDLFloat,
    String as WDLString,
    File as WDLFile,
    Directory as WDLDirectory,
    Int as WDLInt,
    Boolean as WDLBool,
    Array as WDLArray,
    Map as WDLMap,
    Pair as WDLPair,
    StructInstance as WDLStruct,
)

from typing import Optional, Any, Dict, Tuple, List, Union
from WDL.Type import Base as WDLBase

from lib import (
    run_cmd,
    py_type_of_wdl_class,
    get_listing,
    listings_equivalent,
    verify_failure,
    announce_test,
    print_response,
    convert_type,
    run_setup,
    get_specific_tests,
    get_wdl_file,
    verify_return_code,
    test_dependencies,
    WDL_VERSIONS
)

class WDLRunner:
    """
    A class describing how to invoke a WDL runner to run a workflow.
    """
    runner: str

    def format_command(self, wdl_file, json_input, results_file, args, verbose, pre_args=None):
        raise NotImplementedError


class CromwellStyleWDLRunner(WDLRunner):
    def __init__(self, runner):
        self.runner = runner

    def format_command(self, wdl_file: str, json_input: Union[str, Dict[str, Any]], results_file: str,
                       args: List[str], verbose: bool, pre_args: Optional[List[str]] = None) -> List[str]:
        """
        Make a list of command parts to invoke a WDL runner that uses Cromwell-style arguments.

        :param json_input: Can either be a json as a string or a dictionary that is json parseable
        """
        if isinstance(json_input, dict):
            json_input = json.dumps(json_input)
        json_arg = ["-i", json_input] if json_input is not None else []
        return list(filter(None, self.runner.split(" "))) + [wdl_file, "-m", results_file] + json_arg + args


class CromwellWDLRunner(CromwellStyleWDLRunner):
    download_lock = threading.Lock()

    def __init__(self):
        super().__init__('cromwell')

    def format_command(self, wdl_file: str, json_input: Union[str, Dict[str, Any]], results_file: str,
                       args: List[str], verbose: bool, pre_args: Optional[List[str]] = None) -> List[str]:
        if self.runner == 'cromwell' and not which('cromwell'):
            with CromwellWDLRunner.download_lock:
                if self.runner == 'cromwell':
                    # if there is no cromwell binary seen on the path, download
                    # our pinned version and use that instead
                    log_level = '-DLOG_LEVEL=OFF' if not verbose else ''
                    pre_args = '' if pre_args is None else pre_args
                    cromwell = os.path.abspath('build/cromwell.jar')
                    if not os.path.exists(cromwell):
                        print('Cromwell not seen in the path, now downloading cromwell to run tests... ')
                        run_cmd(cmd='make cromwell'.split(" "), cwd=os.getcwd())
                    self.runner = f'java {log_level} {pre_args} -jar {cromwell} run'

        return super().format_command(wdl_file, json_input, results_file, args, verbose)


class MiniWDLStyleWDLRunner(WDLRunner):
    def __init__(self, runner):
        self.runner = runner

    def format_command(self, wdl_file: str, json_input: Union[str, Dict[str, Any]], results_file: str,
                       args: List[str], verbose: bool, pre_args: Optional[List[str]] = None) -> List[str]:
        if isinstance(json_input, dict):
            json_input = json.dumps(json_input)
        json_arg = ["-i", json_input] if json_input is not None else []
        return self.runner.split(" ") + [wdl_file, "-o", results_file, "-d", "miniwdl-logs",
                                         "--verbose"] + json_arg + args


RUNNERS = {
    'cromwell': CromwellWDLRunner(),
    'toil-wdl-runner': CromwellStyleWDLRunner('toil-wdl-runner --outputDialect miniwdl --logDebug'),
    'miniwdl': MiniWDLStyleWDLRunner('miniwdl run')
}


class WDLConformanceTestRunner:
    tests: List[Dict[Any, Any]]

    def __init__(self, conformance_file: str):
        yaml = YAML(typ='safe')
        with open(conformance_file, 'r') as f:
            self.tests = yaml.load(f)

    def compare_outputs(self, expected: Any, result: Any, typ: WDLBase):
        """
        Recursively ensure that the expected output object is the same as the resulting output object

        In the future, make this return where it failed instead to give less generic error messages

        :param expected: expected value object from conformance file
        :param result: result value object from WDL runner
        :param typ: type of output from conformance file
        """
        if typ.optional and expected is None and result is None:
            # an optional result does not need to exist
            return {'status': f'SUCCEEDED'}
        if isinstance(typ, WDLArray):
            try:
                if len(expected) != len(result):
                    # length of output doesn't match
                    return {'status': 'FAILED', 'reason': f"Size of expected and result do not match!\n"
                                                          f"Expected output: {expected}\n"
                                                          f"Actual output: {result}!"}
                for i in range(len(expected)):
                    status_result = self.compare_outputs(expected[i], result[i], typ.item_type)
                    if status_result['status'] == 'FAILED':
                        return status_result
            except TypeError:
                return {'status': 'FAILED', 'reason': f"Not an array!\nExpected output: {expected}\n"
                                                      f"Actual output: {result}"}

        if isinstance(typ, WDLMap):
            try:
                if len(expected) != len(result):
                    return {'status': 'FAILED', 'reason': f"Size of expected and result do not match!\n"
                                                          f"Expected output: {expected}\n"
                                                          f"Actual result was: {result}!"}
                # compare both the key and values of the map
                expected_map_keys, result_map_keys = list(expected.keys()), list(result.keys())
                for i in range(len(expected)):
                    expected_key = expected_map_keys[i]
                    result_key = result_map_keys[i]
                    status_result = self.compare_outputs(expected_key, result_key, typ.item_type[0])
                    if status_result['status'] == 'FAILED':
                        return status_result
                    status_result = self.compare_outputs(expected[expected_key], result[result_key], typ.item_type[1])
                    if status_result['status'] == 'FAILED':
                        return status_result
            except (KeyError, TypeError):
                return {'status': 'FAILED', 'reason': f"Not a map or missing keys!\nExpected output: {expected}\n"
                                                      f"Actual output: {result}"}

        # WDLStruct also represents WDLObject
        # Objects in conformance will be forced to be typed, same as Structs
        if isinstance(typ, WDLStruct):
            nonoptional_result = {k: v for k, v in result.items() if not typ.members[k].optional}
            try:
                if len(expected) < len(nonoptional_result) or len(expected) > len(result):
                    return {'status': 'FAILED', 'reason': f"Size of expected and result are invalid! The outputs likely don't match.\n"
                                                          f"Expected output: {expected}\n"
                                                          f"Actual output: {result}!"}
                for key in expected.keys():
                    status_result = self.compare_outputs(expected[key], result[key], typ.members[key])
                    if status_result['status'] == 'FAILED':
                        return status_result
            except (KeyError, TypeError):
                return {'status': 'FAILED', 'reason': f"Not a struct or missing keys!\nExpected output: {expected}\n"
                                                      f"Actual output: {result}"}

        if isinstance(typ, (WDLInt, WDLFloat, WDLBool, WDLString)):
            # check that outputs are the same
            if expected != result:
                return {'status': 'FAILED', 'reason': f"Expected and result do not match!\n"
                                                      f"Expected output: {expected}\n"
                                                      f"Actual output: {result}!"}
            # check that output types are correct
            expected_type = py_type_of_wdl_class(typ)
            if not isinstance(expected, expected_type) or not isinstance(result, expected_type):
                # When outputting in both miniwdl and toil, Map keys are always strings regardless of the specified type
                # When Map[Int, Int], the output will be {"1": 1}
                # Or when Map[Float, Int], the output will be {"1.000000": 1}
                # This only applies to Int and Float types, as Boolean key types don't seem to be supported in miniwdl
                if isinstance(typ, WDLInt):
                    # Ensure the actual result is parseable as the correct type
                    try:
                        # ensure the stringified version of the runner output is equivalent to an int
                        expected_type(result)
                    except ValueError:
                        # the string representation does not represent the right type
                        return {'status': 'FAILED', 'reason': f"Runner output {result} is not type Int from the conformance file."}
                    # For good measure, ensure the expected result is as well.
                    # TODO: If a test fails because of this it is really a bug in the test definition. (also mirrored below)
                    try:
                        # ensure the stringified version of the conformance output is equivalent to an int
                        expected_type(expected)
                    except ValueError:
                        return {'status': 'FAILED', 'reason': f"Conformance output and type does not match. Expected output {expected} with expected type Int"}

                elif isinstance(typ, WDLFloat):
                    # Ensure the actual result is parseable as the correct type
                    try:
                        # ensure the stringified version of the runner output is equivalent to an float
                        expected_type(result)
                    except ValueError:
                        # the string representation does not represent the right type
                        return {'status': 'FAILED', 'reason': f"Runner output {result} is not type Float from the conformance file."}
                    # For good measure, ensure the expected result is as well.
                    try:
                        # ensure the stringified version of the conformance output is equivalent to an float
                        expected_type(expected)
                    except ValueError:
                        return {'status': 'FAILED', 'reason': f"Conformance output and type does not match. Expected output {expected} with expected type Float"}
                else:
                    if not isinstance(expected, expected_type):
                        return {'status': 'FAILED', 'reason': f"Incorrect types! Runner output {expected} is not of type {str(typ)}\n"}
                    elif not isinstance(result, expected_type):
                        return {'status': 'FAILED', 'reason': f"Incorrect types! Runner output {result} is not of type {str(typ)}\n"}

        if isinstance(typ, WDLFile):
            # check file path exists
            if not os.path.exists(result):
                return {'status': 'FAILED', 'reason': f"Result file does not exist!\n"
                                                      f"Expected file path: {result}!"}

            if not isinstance(expected, dict):
                return {'status': 'FAILED', 'reason': f"Expected value is not a regex or md5sum!\n"
                                                      f"Expected result was: {expected}"}
            regex = expected.get('regex')
            if regex == "":
                return {'status': 'FAILED', 'reason': f"Expected regex is empty!"}
            if regex is not None:
                # get regex
                re_c = re.compile(regex)
                # check against regex
                with open(result, "r") as f:
                    text = f.read()
                    if not re_c.search(text):
                        return {'status': 'FAILED', 'reason': f"Regex did not match!\n"
                                                              f"Regex: {regex}\n"
                                                              f"Value: {text}"}
            else:
                # get md5sum
                with open(result, "rb") as f:
                    md5sum = hashlib.md5(f.read()).hexdigest()
                # check md5sum
                if md5sum != expected['md5sum']:
                    return {'status': 'FAILED', 'reason': f"Expected file does not match!\n"
                                                          f"Expected md5sum: {expected['md5sum']}\n"
                                                          f"Actual md5sum: {md5sum}!"}

        if isinstance(typ, WDLDirectory):
            # check directory path exists
            if not os.path.exists(result):
                return {'status': 'FAILED', 'reason': f"Result directory does not exist!\n"
                                                      f"Expected directory path: {result}!"}

            if not isinstance(expected, dict):
                return {'status': 'FAILED', 'reason': f"Expected value is not a listing!\n"
                                                      f"Expected result was: {expected}"}
            listing = expected.get('listing')
            if not isinstance(listing, list):
                return {'status': 'FAILED', 'reason': f"Expected listing value is not a list!\n"
                                                      f"Expected result was: {expected}"}

            result_listing = get_listing(result)
            if not listings_equivalent(result_listing, listing):
                return {'status': 'FAILED', 'reason': f"Expected listing does not match!\n"
                                                      f"Expected listing: {expected['listing']}\n"
                                                      f"Actual listing: {listing}!"}


        if isinstance(typ, WDLPair):
            try:
                if len(expected) != 2:
                    return {'status': 'FAILED', 'reason': f"Expected value is not a pair!\n"
                                                          f"Expected output: {expected}"}
                if expected['left'] != result['left'] or expected['right'] != result['right']:
                    return {'status': 'FAILED', 'reason': f"Expected and result do not match!\n"
                                                          f"Expected output: {expected}\n"
                                                          f"Actual result was: {result}!"}
            except (KeyError, TypeError):
                return {'status': 'FAILED', 'reason': f"Not a pair or missing keys!\nExpected output: {expected}\n"
                                                      f"Actual output: {result}"}
        return {'status': f'SUCCEEDED'}

    def run_verify(self, expected: dict, results_file: str, ret_code: int) -> dict:
        """
        Check either for proper output or proper success/failure of WDL program, depending on if 'fail' is included in
        the conformance test
        """
        outputs = expected['outputs']
        exclude_outputs = expected.get('exclude_output')

        if expected.get("fail"):
            # workflow is expected to fail
            response = verify_failure(ret_code)
        else:
            # workflow is expected to run
            response = self.verify_outputs(outputs, results_file, ret_code, exclude_outputs)

        if response["status"] == "SUCCEEDED" and expected.get("return_code") is not None:
            # check return code if it exists
            response.update(verify_return_code(expected["return_code"], ret_code))

        return response

    def verify_outputs(self, expected: dict, results_file: str, ret_code: int, exclude_outputs: Optional[list]) -> dict:
        """
        Verify that the test result outputs are the same as the expected output from the conformance file

        :param expected: expected value object
        :param results_file: filepath of resulting output file from the WDL runner
        :param ret_code: return code from WDL runner
        :param exclude_outputs: outputs to exclude when comparing
        """
        try:
            with open(results_file, 'r') as f:
                test_results = json.load(f)
        except OSError:
            # some runners won't create a results file on workflow failure
            # this may not necessarily be an error; failure is only ensured when the config specifies failure
            # and a nonzero exit code is possible on both a failing and successful task
            test_results = {}
        except json.JSONDecodeError:
            return {'status': 'FAILED', 'reason': f'Results file at {results_file} is not JSON'}

        # todo: simplify logic into one branch
        if exclude_outputs is not None and len(exclude_outputs) > 0:
            # I'm not sure if it is possible but the wdl-tests spec seems to say the type can also be a string
            exclude_outputs = [exclude_outputs] if not isinstance(exclude_outputs, list) else exclude_outputs
            # remove the outputs that we are not allowed to compare
            excluded = set(exclude_outputs)
            test_result_outputs = {k: v for k, v in test_results.get('outputs', {}).items() if k.split(".")[-1] not in excluded}

        else:
            test_result_outputs = test_results.get('outputs', {})
        if len(test_result_outputs) != len(expected):
            return {'status': 'FAILED',
                    'reason': f"'outputs' section expected {len(expected)} results ({list(expected.keys())}), got "
                              f"{len(test_result_outputs)} instead ({list(test_result_outputs.keys())}) with exit code {ret_code}"}

        result = {'status': f'SUCCEEDED', 'reason': None}

        # compare expected output to result output
        for identifier, output in test_result_outputs.items():
            try:
                python_type = convert_type(expected[identifier]['type'])
            except KeyError:
                return {'status': 'FAILED',
                        'reason': f"Output variable name '{identifier}' not found in expected results!"}

            if python_type is None:
                return {'status': 'FAILED', 'reason': f"Invalid expected type: {expected[identifier]['type']}!"}

            if 'value' not in expected[identifier]:
                return {'status': 'FAILED', 'reason': f"Test has no expected output of key 'value'!"}
            result = self.compare_outputs(expected[identifier]['value'], output, python_type)
            if result['status'] == 'FAILED':
                return result
        return result

    # Make sure output groups don't clobber each other.
    LOG_LOCK = threading.Lock()

    def run_single_test(self, test_index: int, test: dict, runner: str, version: str, time: bool, verbose: bool,
                        quiet: bool, args: Optional[Dict[str, Any]], jobstore_path: Optional[str], debug: bool) -> dict:
        """
        Run a test and log success or failure.

        Return the response dict.
        """
        if test.get("setup") is not None:
            run_setup(test["setup"])
        inputs = test['inputs']
        wdl_dir = inputs['dir']
        wdl_input = inputs.get('wdl', f'{wdl_dir}.wdl')  # default wdl name
        abs_wdl_dir = os.path.abspath(wdl_dir)
        if version in WDL_VERSIONS:
            wdl_input = f'{wdl_dir}/{wdl_input}'
        else:
            return {'status': 'FAILED', 'reason': f'WDL version {version} is not supported!'}
        wdl_file = os.path.abspath(get_wdl_file(wdl_input, abs_wdl_dir, version))

        json_input = inputs.get('json')
        if json_input is None:
            json_file = None
        else:
            json_path = f'{wdl_dir}/{json_input}'
            json_file = os.path.abspath(json_path)

        json_string = json.dumps(inputs['json_string']) if inputs.get('json_string') else None

        # Only one of json_string or input json file can exist
        if json_string is not None and json_file is not None:
            raise RuntimeError(f'Config file specifies both a json string and json input file! Only one can be supplied! '
                               f'Check the input section for test id {test["id"]}.')

        test_args = args[runner].split(" ") if args[runner] is not None else []
        unique_id = uuid4()
        # deal with jobstore_path argument for toil
        if runner == "toil-wdl-runner" and jobstore_path is not None:
            unique_jobstore_path = os.path.join(jobstore_path, f"wdl-jobstore-{unique_id}")
            test_args.extend(["--jobstore", unique_jobstore_path])
        results_file = os.path.abspath(f'results-{unique_id}.json')
        wdl_runner = RUNNERS[runner]
        # deal with cromwell arguments to define java system properties
        pre_args = None
        if runner == "cromwell":
            pre_args = args["cromwell_pre_args"]

        # todo: it seems odd that I'm looking for a dependency before running when the test spec says test frameworks are supposed to be used to turn failing tests into warnings
        # this is mainly because the docker and singularity strings that I use are custom dependency values that I use
        # but I use it to carry data through on how to run the runner rather than using it to turn failing tests into warnings
        # (one of the tests depends on findmnt on /, which differs between singularity and docker
        # they mainly tell how toil should run to pass the test
        if runner == "toil-wdl-runner" and "dependencies" in test:
            if "docker" in test["dependencies"]:
                test_args.extend(["--container", "docker"])
            if "singularity" in test["dependencies"]:
                test_args.extend(["--container", "singularity"])
        json_input = json_string or json_file
        cmd = wdl_runner.format_command(wdl_file, json_input, results_file, test_args, verbose, pre_args)

        realtime = None
        if time:
            realtime_start = timeit.default_timer()
            (ret_code, stdout, stderr) = run_cmd(cmd=cmd, cwd=os.path.dirname(os.path.abspath(__file__)), debug=debug)
            realtime_end = timeit.default_timer()
            realtime = realtime_end - realtime_start
        else:
            (ret_code, stdout, stderr) = run_cmd(cmd=cmd, cwd=os.path.dirname(os.path.abspath(__file__)), debug=debug)

        if verbose:
            with self.LOG_LOCK:
                announce_test(test_index, test, version, runner)
        response = self.run_verify(test, results_file, ret_code)

        if response["status"] == "FAILED" and test.get("priority") == "optional":
            # an optional test can be reported as a warning if it fails
            response["status"] = "WARNING"

        if time:
            response['time'] = {"real": realtime}
        if not quiet and (verbose or response['status'] == 'FAILED'):
            response['stdout'] = stdout.decode("utf-8", errors="ignore")
            response['stderr'] = stderr.decode("utf-8", errors="ignore")
        return response

    def handle_test(self, test_index: int, test: Dict[str, Any], runner: str, version: str, time: bool,
                    verbose: bool, quiet: bool, args: Optional[Dict[str, Any]], jobstore_path: Optional[str],
                    repeat: Optional[int] = None, progress: bool = False, debug: bool = False) -> Dict[str, Any]:
        """
        Decide if the test should be skipped. If not, run it.

        Returns a result that can have status SKIPPED, SUCCEEDED, or FAILED.
        """
        response = {'description': test.get('description'), 'number': test_index, 'id': test.get('id')}
        # priority flag is defined in https://github.com/openwdl/wdl-tests/blob/main/docs/Specification.md#test-configuration
        if test.get("priority") == "ignore":
            # config specifies to ignore this test, so skip
            if progress:
                print(f"Ignoring test {test_index} (ID: {test['id']}) with runner {runner} on WDL version {version}.")
            response.update({'status': 'IGNORED'})
            if verbose:
                response.update({'reason': f'Priority of the test is set to ignore.'})
        if version not in test['versions']:
            # Test to skip, if progress is true, then output
            if progress:
                print(f"Skipping test {test_index} (ID: {test['id']}) with runner {runner} on WDL version {version}.")
            response.update({'status': 'SKIPPED'})
            # return reason only if verbose is true
            if verbose:
                response.update({'reason': f'Test only applies to versions: {",".join(test["versions"])}'})
            return response
        else:
            # New test to run, if progress is true, then output
            if progress:
                print(f"Running test {test_index} (ID: {test['id']}) with runner {runner} on WDL version {version}.")
            response.update(
                self.run_single_test(test_index, test, runner, version, time, verbose, quiet, args, jobstore_path, debug))
        if repeat is not None:
            response["repeat"] = repeat
        # Turn failing tests to warnings if any of the tests' dependencies were not
        # actually available.
        response.update(test_dependencies(dependencies=test.get("dependencies"), current_result=response))
        return response

    def _debug_run_all_tests(self, options: argparse.Namespace, args: Optional[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Meant to be called by _run_debug. Runs the tests single-threaded to be compatible with pycharm's debugger.
        """
        print(f"===DEBUG===")
        versions_to_test = set(options.versions.split(','))
        selected_tests = get_specific_tests(conformance_tests=self.tests, options=options)
        selected_tests_amt = len(selected_tests) * len(versions_to_test) * options.repeat
        test_responses = list()
        completed_count = 0
        for test_index in selected_tests:
            try:
                test = self.tests[test_index]
            except KeyError:
                print(f'ERROR: Provided test [{test_index}] do not exist.')
                sys.exit(1)
            result = {}
            for version in versions_to_test:
                for iteration in range(options.repeat):
                    # Handle each test as a concurrent job
                    # todo: abstract below and the other run function so code is deduplicated
                    result = self.handle_test(
                        test_index,
                        test,
                        options.runner,
                        version,
                        options.time,
                        options.verbose,
                        options.quiet,
                        args,
                        options.jobstore_path,
                        iteration + 1 if options.repeat is not None else None,
                        options.progress,
                        options.debug)
                    test_responses.append(result)
            completed_count += 1
            if options.progress:
                print(
                    f"{completed_count}/{selected_tests_amt}. Test {result['number']} (ID: {result['id']}) completed "
                    f"with status {result['status']}. "
                )
        print(completed_count, len(test_responses), test_responses)
        return test_responses

    def run_all_tests(self, options: argparse.Namespace, args: Optional[Dict[str, Any]]):
        """
        Run all tests and capture the test results. Runs in a threaded manner depending on options.threads
        """
        test_responses = list()

        versions_to_test = set(options.versions.split(','))
        selected_tests = get_specific_tests(conformance_tests=self.tests, options=options)
        selected_tests_amt = len(selected_tests) * len(versions_to_test) * options.repeat
        with ProcessPoolExecutor(max_workers=options.threads) as executor:  # process instead of thread so realtime works
            pending_futures = []
            for test_index in selected_tests:
                try:
                    test = self.tests[test_index]
                except KeyError:
                    print(f'ERROR: Provided test [{test_index}] do not exist.')
                    sys.exit(1)
                for version in versions_to_test:
                    for iteration in range(options.repeat):
                        # Handle each test as a concurrent job
                        result_future = executor.submit(self.handle_test,
                                                        test_index,
                                                        test,
                                                        options.runner,
                                                        version,
                                                        options.time,
                                                        options.verbose,
                                                        options.quiet,
                                                        args,
                                                        options.jobstore_path,
                                                        iteration + 1 if options.repeat is not None else None,
                                                        options.progress,
                                                        options.debug)
                        pending_futures.append(result_future)
            completed_count = 0
            for result_future in as_completed(pending_futures):
                completed_count += 1
                # Go get each result
                result = result_future.result()
                test_responses.append(result)
                if options.progress:
                    # if progress is true, then print a summarized output of the completed test and current status
                    print(
                        f"{completed_count}/{selected_tests_amt}. Test {result['number']} (ID: {result['id']}) completed "
                        f"with status {result['status']}. "
                    )
        return test_responses

    def run_and_generate_tests_args(self, options: argparse.Namespace, args: Optional[Dict[str, Any]]) -> Tuple[List[Any], bool]:
        # Get all the versions to test.
        # Unlike with CWL, WDL requires a WDL file to declare a specific version,
        # and prohibits mixing file versions in a workflow, although some runners
        # might allow it.
        # But the tests all need to be for single WDL versions.
        versions_to_test = set(options.versions.split(','))
        selected_tests = get_specific_tests(conformance_tests=self.tests, options=options)
        selected_tests_amt = len(selected_tests) * len(versions_to_test) * options.repeat
        successes = 0
        skips = 0
        ignored = 0
        warnings = 0
        failed = 0
        print(f'Testing runner {options.runner} on WDL versions: {",".join(versions_to_test)}\n')

        if options.debug is True:
            test_responses = self._debug_run_all_tests(options, args)
        else:
            test_responses = self.run_all_tests(options, args)

        print("\n=== REPORT ===\n")

        # print tests in order to improve readability
        test_responses.sort(key=lambda a: a['number'])
        for response in test_responses:
            print_response(response)
            if response['status'] == 'SUCCEEDED':
                successes += 1
            elif response['status'] == 'SKIPPED':
                skips += 1
            elif response['status'] == 'IGNORED':
                ignored += 1
            elif response['status'] == 'WARNING':
                warnings += 1
            elif response['status'] == 'FAILED':
                failed += 1

        print(
            f'{selected_tests_amt - skips} tests run, {successes} succeeded, {failed} '
            f'failed, {skips} skipped, {ignored} ignored, {warnings} warnings'
        )

        # identify the failing tests
        failed_ids = [str(response['number']) for response in test_responses if
                      response['status'] in {'FAILED'}]
        warn_ids = [str(response['number']) for response in test_responses if
                    response['status'] in {'WARNING'}]
        if len(failed_ids) > 0:
            print(f"\tFailures: {','.join(failed_ids)}")
        else:
            print("\tNo failures!")
        if len(warn_ids) > 0:
            print(f"\tWarnings: {','.join(warn_ids)}")

        if len(failed_ids) > 0:
            return test_responses, False
        else:
            return test_responses, True

    def run_and_generate_tests(self, options: argparse.Namespace) -> Tuple[List[Any], bool]:
        """
        Call run_and_generate_tests_args with a namespace object
        """
        args = {}
        for runner in RUNNERS.keys():
            if runner == "miniwdl":
                args[runner] = options.miniwdl_args
            if runner == "toil-wdl-runner":
                args[runner] = options.toil_args
            if runner == "cromwell":
                args[runner] = options.cromwell_args
                args["cromwell_pre_args"] = options.cromwell_pre_args

        return self.run_and_generate_tests_args(options=options, args=args)


def add_options(parser) -> None:
    """
    Add options to a parser
    """
    parser.add_argument("--verbose", default=False, action='store_true',
                        help='Print more information about a test')
    parser.add_argument("--versions", "-v", default="1.0", choices=WDL_VERSIONS,
                        help='Select the WDL versions you wish to test against. Ex: -v=draft-2,1.0')
    parser.add_argument("--tags", "-t", default=None,
                        help='Select the tags to run specific tests')
    parser.add_argument("--numbers", "-n", default=None,
                        help='Select the WDL test numbers you wish to run. Can be a comma separated list or hyphen '
                             'separated inclusive ranges. Ex: -n=1-4,6,8-10')
    parser.add_argument("--runner", "-r", default='cromwell', choices=["cromwell", "toil-wdl-runner", "miniwdl"],
                        help='Select the WDL runner to use.')
    parser.add_argument("--threads", type=int, default=1,
                        help='Number of tests to run in parallel. The maximum should be the number of CPU cores (not '
                             'threads due to wall clock timing).')
    parser.add_argument("--time", default=False, action="store_true",
                        help="Time the conformance test run.")
    parser.add_argument("--quiet", default=False, action="store_true")
    parser.add_argument("--exclude-numbers", default=None, help="Exclude certain test numbers.")
    parser.add_argument("--exclude-tags", default=None, help="Exclude certain test tags.")
    parser.add_argument("--conformance-file", default="conformance.yaml",
                        help="Run the given test suite.")
    parser.add_argument("--toil-args", default=None, help="Arguments to pass into toil-wdl-runner. Ex: "
                                                          "--toil-args=\"caching=False\"")
    parser.add_argument("--miniwdl-args", default=None, help="Arguments to pass into miniwdl. Ex: "
                                                             "--miniwdl-args=\"--no-outside-imports\"")
    parser.add_argument("--cromwell-args", default=None, help="Arguments to pass into cromwell. Ex: "
                                                              "--cromwell-args=\"--options=[OPTIONS]\"")
    parser.add_argument("--cromwell-pre-args", default=None, help="Arguments to set java system properties before "
                                                                  "calling cromwell. This allows things such as "
                                                                  "setting cromwell config files with "
                                                                  "--cromwell-pre-args="
                                                                  "\"-Dconfig.file=build/overrides.conf\".")
    parser.add_argument("--id", default=None, help="Specify WDL tests by ID.")
    parser.add_argument("--repeat", default=1, type=int, help="Specify how many times to run each test.")
    # This is to deal with jobstores being created in the /data/tmp directory on Phoenix, which appears to be unique
    # per worker, thus causing JobstoreNotFound exceptions when running across multiple workers
    parser.add_argument("--jobstore-path", "-j", default=None, help="Specify the PARENT directory for the jobstores to "
                                                                    "be created in.")
    # Test responses are collected and sorted, so this option allows the script to print out the current progress
    parser.add_argument("--progress", default=False, action="store_true", help="Print the progress of the test suite "
                                                                               "as it runs.")
    parser.add_argument("--debug", action="store_true", default=False, help="Specifically to be used with Pycharm's debugger (or a pgdb based debugger)."
                                                                            "This makes everything run on the same thread.")


def main(argv=None):
    # get directory of conformance tests and store as environmental variable
    # used to specify absolute paths in conformance file
    if argv is None:
        argv = sys.argv[1:]
    parser = argparse.ArgumentParser(description='Run WDL conformance tests.')
    add_options(parser)
    argcomplete.autocomplete(parser)
    args = parser.parse_args(argv)

    if args.runner not in RUNNERS:
        print(f'Unsupported runner: {args.runner}')
        sys.exit(1)

    conformance_runner = WDLConformanceTestRunner(conformance_file=args.conformance_file)
    _, successful_run = conformance_runner.run_and_generate_tests(args)
    if not successful_run:
        # Fail the program overall if tests failed.
        sys.exit(1)


if __name__ == '__main__':
    main()
