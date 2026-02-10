version 1.0
task lima_demultiplex {
        input {
                File reads_ccs_bam
                File isoseq_primers_fasta
                String bool
        }

        String reads_demult_bam = "lima_output.fastq"
        String prefix = sub(reads_demult_bam, ".bam", "")
	
        command <<<
                if [["~{bool}" = "true"]]; then
                lima --dump-clips --peek-guess -j 16 ~{reads_ccs_bam} ~{isoseq_primers_fasta} ~{reads_demult_bam}
                else
                mv ~{reads_ccs_bam} "lima_output.5p--3p.bam"
                fi
        >>>

        output {
                File fivep__3p_bam = "lima_output.5p--3p.bam"
        }

        runtime {
                docker: "developmentontheedge/lima:0.1"
        }
}

workflow lima {
        input {
                File reads_ccs_bam
                File isoseq_primers_fasta
                String bool
        }

        call lima_demultiplex {
                input:
                bool = bool,
                reads_ccs_bam=reads_ccs_bam,
                isoseq_primers_fasta=isoseq_primers_fasta
        }

        output {
                File fivep__3p_bam = lima_demultiplex.fivep__3p_bam
        }

        meta {
                description: "##"
        }
}
