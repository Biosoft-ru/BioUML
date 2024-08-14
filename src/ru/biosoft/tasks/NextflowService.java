package ru.biosoft.tasks;

import java.util.Map;

import org.json.JSONObject;

public interface NextflowService
{
    //response looks like
    //{"runname":"id-ecd2ff19-72de-4908-b41c-cd1ff923b9a6","user":"nbgi","nativeid":7079,"status":"STARTED","errmsg":null,"launchdir":"/var/nfwork/id-ecd2ff19-72de-4908-b41c-cd1ff923b9a6"}
    //The runname and status - obligatory fields
    //the error response example:
    //{"timestamp":"2022-02-01T09:23:41.248+00:00","status":400,"error":"Bad Request","message":"","path":"/nfcontrol/api/start"}
    JSONObject submit(String nextflowScriptPath, String[] params) throws Exception;
    
    
    //{"header":{"runid":"id-8758eca6-a2e0-4795-a09f-0e11302d128f","sessionid":"f4414f13-d0b6-4c82-bbc3-94172fdc2916","event":"completed","username":"nbgi","runname":"id-8758eca6-a2e0-4795-a09f-0e11302d128f","start":"2022-02-01T09:25:51.000+00:00","complete":"2022-02-01T09:26:03.000+00:00","projectdir":"/var/nfwork/scripts","cmdline":"nextflow -trace nextflow run -c /var/nfwork/id-8758eca6-a2e0-4795-a09f-0e11302d128f/nextflow.config /var/nfwork/scripts/fasta.nf --fasta /var/nfwork/input/sample.fa -with-weblog 'http://nfmonitor-service:3131/nfcontrol/api/weblog' -name id-8758eca6-a2e0-4795-a09f-0e11302d128f","scriptfile":"/var/nfwork/scripts/fasta.nf","success":true,"duration":11924,"runstatus":"STARTED","runnativeid":8512,"launchdir":"/var/nfwork/id-8758eca6-a2e0-4795-a09f-0e11302d128f","errorMessage":"tput: unknown terminal \"unknown\"\ntput: unknown terminal \"unknown\"\ntput: unknown terminal \"unknown\"\ntput: unknown terminal \"unknown\"\ntput: unknown terminal \"unknown\"\n"},"workflowProcessList":[{"traceid":9941,"runid":"f4414f13-d0b6-4c82-bbc3-94172fdc2916","task_id":1,"status":"COMPLETED","hash":"ef/1e117a","name":"run_seqkit (1)","exit":0,"submit":1643707554209,"start":1643707561000,"complete":1643707561000,"process":"run_seqkit","container":"login1.sandbox.g3.computing.kiae.ru/seqkit","script":"seqkit stats sample.fa","workdir":"/var/nfwork/id-8758eca6-a2e0-4795-a09f-0e11302d128f/work/ef/1e117a219498ddb4b90a92efe13a16","queue":null,"cpus":1.0,"memory":0.0,"disk":0.0,"duration":6791,"native_id":"nf-ef1e117a219498ddb4b90a92efe13a16","stdOut":"file       format  type  num_seqs  sum_len  min_len  avg_len  max_len\nsample.fa  FASTA   DNA          1    1,050    1,050    1,050    1,050\n","stdErr":""}]}
    JSONObject status(String runname) throws Exception; 
}
