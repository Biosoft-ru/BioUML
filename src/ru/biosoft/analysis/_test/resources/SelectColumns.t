=LoadTable	test/input
ID	num:Integer	txt:Text
test1	1	aa
test2	2	bb
test3	3	cc
test4	4	dd
test5	5	ee
=End

=Analysis	ru.biosoft.analysis.SelectColumns
columnGroup	["test/input",["txt",0]]
output	test/output
=End

=LoadTable	test/expected
ID	txt:Text
test1	aa
test2	bb
test3	cc
test4	dd
test5	ee
=End

=CompareTables
test/output
test/expected
=End