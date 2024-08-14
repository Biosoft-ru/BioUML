=LoadTable	test/input1
ID	data1:Text	data2:Float
1	abc	1.12
2	cde	2.24
3	efg	3.36
=End

=LoadTable	test/input2
ID	data3:Text	data2:Float
3	qqq	10
2	www	20
5	eee	30
=End

=Analysis	ru.biosoft.analysis.JoinTable
leftGroup/tablePath	test/input1
rightGroup/tablePath	test/input2
joinType	0	(inner join)
mergeColumns	false
output	test/output
=End

=LoadTable	test/expected1
ID	data1:Text	data2 input1:Float	data2 input2:Float	data3:Text
3	efg	3.36	10	qqq
2	cde	2.24	20	www
=End

=CompareTables
test/output
test/expected1
=End

=Analysis	ru.biosoft.analysis.JoinTable
leftGroup/tablePath	test/input1
rightGroup/tablePath	test/input2
joinType	1	(outer join)
mergeColumns	false
output	test/output2
=End

=LoadTable	test/expected2
ID	data1:Text	data2 input1:Float	data2 input2:Float	data3:Text
3	efg	3.36	10	qqq
2	cde	2.24	20	www
1	abc	1.12
5	(null)	(null)	30	eee
=End

=CompareTables
test/output2
test/expected2
=End

=Analysis	ru.biosoft.analysis.JoinTable
leftGroup/tablePath	test/input1
rightGroup/tablePath	test/input2
joinType	0	(inner join)
mergeColumns	true
aggregator	sum
output	test/output3
=End

=LoadTable	test/expected3
ID	data1:Text	data2:Float	data3:Text
3	efg	13.36	qqq
2	cde	22.24	www
=End

=CompareTables
test/output3
test/expected3
=End
