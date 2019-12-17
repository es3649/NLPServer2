build:
	./tools/compile.sh

start:
	sudo ./run.sh com.studmane.nlpserver.Server

javadoc:
	./tools/compiledocs.sh
	# scp -r docs/* seric@schizo.cs.byu.edu:~/public_html/fms/docs/

copy_schema:
	# scp schema.html seric@schizo.cs.byu.edu:~/public_html/fms/docs/

junit:
	./tools/runJunitTests.sh

clean:
	rm -rf bin/*

jdb:
	./tools/openJDB.sh

stats:
	@echo "File count:"
	@find -name "*.java" | wc -l
	@echo "Total line count:"
	@find -name "*.java" | xargs cat | wc -l

# old junit:
# java -cp ".:bin/:libs/jars/sqlite-jdbc-3.7.2.jar:./libs/jars/gson-2.6.2.jar:./libs/jars/json-20160212.jar:./libs/jars/junit-4.12.jar:./libs/jars/hamcrest-core-1.3.jar:" org.junit.runner.JUnitCore ${ARGS}

# run:
# java --class-path "./bin/:./libs/jars/sqlite-jdbc-3.7.2.jar:./libs/jars/gson-2.6.2.jar:./libs/jars/json-20160212.jar:./libs/jars/junit-4.12.jar:./libs/jars/hamcrest-core-1.3.jar:"
