=LoadTable	test/input1
ID	data1:Text	data2:Float
1	abc	1.12
2	cde	2.24
3	efg	3.36
=End

=LoadTable	test/input2
ID	data3:Text	data4:Float
3	qqq	10
2	www	20
5	eee	30
=End

=LoadTable	test/input3
ID	data5:Text	data6:Float
7	aa	5
2	bb	10
5	cc	15
=End

=Analysis	ru.biosoft.analysis.MultipleTableJoin
tablePaths	test/input1;input2;input3
joinType	0	(inner join)
outputPath	test/output
=End

=LoadTable	test/expected1
ID	data1:Text	data2:Float	data3:Text	data4:Float	data5:Text	data6:Float
2	cde	2.24	www	20	bb	10
=End

=CompareTables
test/output
test/expected1
=End
