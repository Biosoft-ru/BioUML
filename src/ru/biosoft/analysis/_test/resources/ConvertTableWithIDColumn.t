=LoadTable	test/inputAvg
ID	Val1	Val2:Float
1	A	10
2	A	20
3	B	30
4	C,D	40
5	D	50
=End

=LoadTable	test/resultAvg
ID	Unspecified ID	Val1	Val2:Float
A	1,2	A	15
B	3	B	30
C	4	C,D	40
D	4,5	C,D	45
=End

=Analysis	ru.biosoft.analysis.TableConverter
sourceTable	test/inputAvg
idsColumnName	Val1
sourceType	Unspecified
targetType	Unspecified
aggregator	average
outputTable	test/outputAvg
=End

=CompareTables
test/outputAvg
test/resultAvg
=End

=LoadTable	test/inputAvgNO
ID	Val1	Val2:Float
1	A	0
2	A	20
3	A	30
4	A	40
5	A	20
6	A	30
7	A	40
8	A	30
9	A	30
10	A	1000
=End

=LoadTable	test/resultAvgNO
ID	Unspecified ID	Val1	Val2:Float
A	1,10,2,3,4,5,6,7,8,9	A	30
=End

=Analysis	ru.biosoft.analysis.TableConverter
sourceTable	test/inputAvgNO
idsColumnName	Val1
sourceType	Unspecified
targetType	Unspecified
aggregator	average w/o 20% outliers
outputTable	test/outputAvgNO
=End

=CompareTables
test/outputAvgNO
test/resultAvgNO
=End

=LoadTable	test/inputMax
ID	Val1	Val2:Float
1	A	0
2	A	20
3	A	30
4	B	40
5	B	20
6	B	30
7	C	40
8	C	30
9	C	30
10	C	1000
=End

=LoadTable	test/resultMax
ID	Unspecified ID	Val1	Val2:Float
A	3	A	30
B	4	B	40
C	10	C	1000
=End

=Analysis	ru.biosoft.analysis.TableConverter
sourceTable	test/inputMax
idsColumnName	Val1
sourceType	Unspecified
targetType	Unspecified
aggregator	maximum
columnName	Val2
outputTable	test/outputMax
=End

=CompareTables
test/outputMax
test/resultMax
=End

=LoadTable	test/inputMin
ID	Val1	Val2:Float
1	A	0
2	A	20
3	A	30
4	B	40
5	B	20
6	B	30
7	C	40
8	C	30
9	C	35
10	C	1000
=End

=LoadTable	test/resultMin
ID	Unspecified ID	Val1	Val2:Float
A	1	A	0
B	5	B	20
C	8	C	30
=End

=Analysis	ru.biosoft.analysis.TableConverter
sourceTable	test/inputMin
idsColumnName	Val1
sourceType	Unspecified
targetType	Unspecified
aggregator	minimum
columnName	Val2
outputTable	test/outputMin
=End

=CompareTables
test/outputMin
test/resultMin
=End
