#!/usr/bin/env nextflow
nextflow.enable.dsl=2
def length(arr) {
    if (arr instanceof java.util.List)
        return arr.size()
    else 
        return arr.count()
}

def range(n) {
    if (n instanceof Integer)
    {
        return Channel.of(0..<n)
    } 
    else
    {
        return n.map { count -> 0..<count }.flatMap { it }
    }
}

// Process equivalent to FirstTask
process FirstTask {
     
	  publishDir "double_scatter_range/firsttask", mode: 'copy'
    input:
    val i
    
    output:
    path "element_${i}.txt", emit: element
    
    script:
    """
    echo ${i} > element_${i}.txt
    """
}

process FirstTask2 {
     
	  publishDir "double_scatter_range/firsttask2", mode: 'copy'
    input:
    val i
    
    output:
    path "element_${i}.txt", emit: element
    
    script:
    """
    echo ${i} > element_${i}.txt
    """
}


def numerate(ch) {
    def counter = -1
    return ch.map { sublist ->
        counter++
        tuple(counter, sublist)
    }
}

workflow {

    def F = [[1,2,3],[4,5,6],[7,8,9]]
	F_ch = Channel.from(F)
	numerated = numerate(F_ch)
	
	parts = numerated.multiMap{ i, val -> 
	    index: i
		value: val
		pair: [ i, val ]
	}
	
	i_ch = range(length(F))
	
	map = i_ch.multiMap{ i->
	   def data= F[i]
	   def j_ch = range(length(data))  
	   
	   data: data
	   j_ch: j_ch
	}
	
	map.data.view()
	//range(length(F_ch)).multiMap
	
	//parts.pair.multiMap{ 
	//
	//numerated.multiMap{ i, val -> {
	//    def data = val
	
	//}
	//range(length(F_ch))
uiiiijj)
	
	//FirstTask2(parts.index.map { i->i*3})
}
	
	
	//scatter (i in range(length(F))) {
    //Array[Int] data = F[i]
	// scatter (j in range(length(data))) {
	//call FirstTask {
    //     input: element = data[j]
    //}
	//}
	 
    //scatter_2 = range(length(scatter_1_mapped.value))

   // scatter_2_mapped = scatter_2.multiMap { j ->
   //     FirstTask_input: scatter_1_mapped.value[j]
   // }

//scatter_2_mapped.FirstTask_input.view()
    //FirstTalk_result = FirstTask(scatter_2.FirstTask_input)
    //FirstTalk_result.element.view()
//}

    // scatter (i in range(length(F)))
    // Array[Int] data = F[i]
    //range(length(F))
     //   .map { i -> [i, F[i]] }
     ////   .multiMap { i, data ->
     //       indices: [i, length(data)]
     //       arrays: [i, data]
      //  }
     //   .set { outer }
    
    // scatter (j in range(length(data)))
    //outer.indices
    //    .flatMap { i, len ->
            // Generate [i, j] pairs for each j in range(length(data))
     //       (0..<len).collect { j -> [i, j] }
     //   }
     //   .combine(outer.arrays, by: 0)
     //   .map { i, j, data -> 
    //        // element = data[j]
    //        [i, data[j]]
     //   }
    //    .set { first_task_input }
    
    // call FirstTask
    //FirstTask(first_task_input)
    
    // output: Array[Array[File]] all_files = FirstTask.out_file
    //FirstTask.out
    //    .groupTuple(by: 0)
    //    .map { i, files -> files }
    //    .collect()
    //    .view { all_files ->
    //        "All output files (nested structure): ${all_files}"
    //    }
//}