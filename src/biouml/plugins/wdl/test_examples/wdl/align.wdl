version 1.0

struct FastqPair {
    File read_1
    File? read_2
    String? read_group
}

struct BamAndLigationCount {
    File bam
    File ligation_count
    Boolean single_ended
}

struct RuntimeEnvironment {
    String docker
    String singularity
}

workflow hic {
    meta {
        caper_docker: "encodedcc/hic-pipeline:1.15.1"
        caper_singularity: "docker://encodedcc/hic-pipeline:1.15.1"
        croo_out_def: "https://raw.githubusercontent.com/ENCODE-DCC/hic-pipeline/dev/croo_out_def.json"
        description: "ENCODE Hi-C pipeline, see https://github.com/ENCODE-DCC/hic-pipeline for details."
    }

    input {
        # Main entrypoint, need to specify all five of these values except read_groups when running from fastqs
        Array[Array[FastqPair]] fastq = []
        Array[String] restriction_enzymes = []
        String? ligation_site_regex
        File? restriction_sites
        File? reference_index

        # Entrypoint for loop and TAD calls
        File? input_hic

        # Resource parameters
        Int align_num_cpus = 32
        Int align_ram_gb_in_situ = 64
        Int align_ram_gb_intact = 88
        Int align_disk_size_gb_in_situ = 1000
        Int align_disk_size_gb_intact = 1500

        String docker = "encodedcc/hic-pipeline:1.15.1"
        String singularity = "docker://encodedcc/hic-pipeline:1.15.1"
        String delta_docker = "encodedcc/hic-pipeline:1.15.1_delta"
        String hiccups_docker = "encodedcc/hic-pipeline:1.15.1_hiccups"
    }

    RuntimeEnvironment runtime_environment = {
      "docker": docker,
      "singularity": singularity
    }

    RuntimeEnvironment hiccups_runtime_environment = {
      "docker": hiccups_docker,
      "singularity": singularity
    }

    RuntimeEnvironment delta_runtime_environment = {
      "docker": delta_docker,
      "singularity": singularity
    }

    Int align_ram_gb = if intact then align_ram_gb_intact else align_ram_gb_in_situ
    Int align_disk_size_gb = if intact then align_disk_size_gb_intact else align_disk_size_gb_in_situ
    Boolean is_nonspecific = length(restriction_enzymes) > 0 && restriction_enzymes[0] == "none"
	
	  if (!defined(input_hic)) {
        if (!defined(ligation_site_regex)) {
            call get_ligation_site_regex { input:
                restriction_enzymes = restriction_enzymes,
                runtime_environment = runtime_environment,
            }
        }

        String ligation_site = select_first([ligation_site_regex, get_ligation_site_regex.ligation_site_regex])

        if (!is_nonspecific && !defined(restriction_sites)) {
            call exit_early { input:
                message = "Must provide restriction sites file if enzyme is not `none`",
                runtime_environment = runtime_environment,
            }
        }
    }
	
	 call align { input:
                fastq_pair = fastq[0][0],
                idx_tar = select_first([reference_index]),
                ligation_site = select_first([ligation_site]),
                num_cpus = align_num_cpus,
                ram_gb = align_ram_gb,
                disk_size_gb = align_disk_size_gb,
                runtime_environment = runtime_environment,
            }
        }
 }
 
task align {
    input {
        FastqPair fastq_pair
        File idx_tar        # reference bwa index tar
        String ligation_site
        Int num_cpus = 32
        Int ram_gb = 64
        Int disk_size_gb = 1000
        RuntimeEnvironment runtime_environment
    }

    command {
        set -euo pipefail
        echo "Starting align"
        mkdir reference
        cd reference && tar -xvf ${idx_tar}
        index_folder=$(ls)
        reference_fasta=$(ls | head -1)
        reference_folder=$(pwd)
        reference_index_path=$reference_folder/$reference_fasta
        cd ..

        usegzip=1
        name="result"
        ligation="${ligation_site}"
        name1=${fastq_pair.read_1}
        name2=${fastq_pair.read_2}
        ext=""
        singleend=~{if(defined(fastq_pair.read_2)) then "0" else "1"}
        #count ligations
        # Need to unset the -e option, when ligation site is XXXX grep will exit with
        # non-zero status
        set +e
        source /opt/scripts/common/countligations.sh
        set -e
        # Align reads
        echo "Running bwa command"
        bwa \
            mem \
            ~{if defined(fastq_pair.read_2) then "-SP5M" else "-5M"} \
            ~{if defined(fastq_pair.read_group) then "-R '" + fastq_pair.read_group + "'" else ""} \
            -t ~{num_cpus} \
            -K 320000000 \
            $reference_index_path \
            ${fastq_pair.read_1} \
            ~{default="" fastq_pair.read_2} | \
            samtools view -hbS - > aligned.bam
        mv result_norm.txt.res.txt ligation_count.txt
    }


    output {
        BamAndLigationCount bam_and_ligation_count = object {
            bam: "aligned.bam",
            ligation_count: "ligation_count.txt",
            single_ended: length(select_all([fastq_pair.read_2])) == 0
        }
     }

    runtime {
        cpu : "~{num_cpus}"
        memory: "~{ram_gb} GB"
        disks: "local-disk ~{disk_size_gb} HDD"
        docker: runtime_environment.docker
        singularity: runtime_environment.singularity
    }
}

task get_ligation_site_regex {
    input {
        Array[String] restriction_enzymes
        RuntimeEnvironment runtime_environment
    }

    String output_path = "ligation_site_regex.txt"

    command <<<
        set -euo pipefail
        python3 "$(which get_ligation_site_regex.py)" \
            --enzymes ~{sep=" " restriction_enzymes} \
            --outfile ~{output_path}
    >>>

    output {
        String ligation_site_regex = read_string("~{output_path}")
        # Surface the original file for testing purposes
        File ligation_site_regex_file = "~{output_path}"
    }

    runtime {
        cpu : "1"
        memory: "500 MB"
        docker: runtime_environment.docker
        singularity: runtime_environment.singularity
    }
}

task exit_early {
    input {
        String message
        RuntimeEnvironment runtime_environment
    }

    command <<<
        set -euo pipefail
        echo ~{message}
        exit 1
    >>>

    runtime {
        cpu : "1"
        memory: "500 MB"
        docker: runtime_environment.docker
        singularity: runtime_environment.singularity
    }
}
