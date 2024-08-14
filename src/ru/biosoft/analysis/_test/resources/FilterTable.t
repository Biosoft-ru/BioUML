=LoadTable	test/input
ID	num+num:Integer
test1	1
test2	2
test3	3
test4	4
test5	5
=End

=LoadTable	test/result1
ID	num+num:Integer
test1	1
test2	2
test3	3
=End

=LoadTable	test/result2
ID	num+num:Integer
=End

=LoadTable	test/result3
ID	num+num:Integer
test4	4
test5	5
=End

=Analysis	ru.biosoft.analysis.FilterTable
inputPath	test/input
outputPath	test/output
filterExpression	num_num*$["num+num"]<10
=End

=CompareTables
test/output
test/result1
=End

=Analysis	ru.biosoft.analysis.FilterTable
inputPath	test/input
outputPath	test/output
filterExpression	num_num*num_num>50
=End

=CompareTables
test/output
test/result2
=End

=Analysis	ru.biosoft.analysis.FilterTable
inputPath	test/result2
outputPath	test/output
filterExpression	true
=End

=CompareTables
test/output
test/result2
=End

=Analysis	ru.biosoft.analysis.FilterTable
inputPath	test/input
outputPath	test/output
filterExpression	num_num
filteringMode	1
valuesCount	2
=End

=CompareTables
test/output
test/result3
=End

=Analysis	ru.biosoft.analysis.FilterTable
inputPath	test/input
outputPath	test/output
filterExpression	num_num*num_num
filteringMode	2
valuesCount	3
=End

=CompareTables
test/output
test/result1
=End

=Analysis	ru.biosoft.analysis.FilterTable
inputPath	test/input
outputPath	test/output
filterExpression	num_num*num_num
filteringMode	1
valuesCount	10
=End

=CompareTables
test/output
test/input
=End

