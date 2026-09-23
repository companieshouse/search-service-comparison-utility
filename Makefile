service_name       := search-service-comparison-utility
version             := "unversioned"
artifact_name := $(service_name)-$(version)
shadedClassifierName := -lambda

.PHONY: all
all: clean build

.PHONY: clean
clean:
	@echo "Running clean"
	mvn clean
	rm -f ./$(artifact_name)*.jar
	rm -rf ./build-*
	rm -f ./build.log
	@echo "Finished clean"

.PHONY: build
build:
	mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	mvn package -DskipTests=true
	cp ./target/$(artifact_name)${shadedClassifierName}.jar .
	@echo "Finished build"

.PHONY: test
test: test-unit

.PHONY: test-unit
test-unit: clean
	mvn test

.PHONY: test-integration
test-integration: clean
	mvn verify -Dskip.unit.tests=true

.PHONY: package
package:
ifndef version
	$(error No version given. Aborting)
endif
	mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	$(info Packaging version: $(version))
	@test -s ./$(artifact_name)$(shadedClassifierName).jar || { echo "ERROR: Service JAR not found"; exit 1; }
	cp ./$(artifact_name)$(shadedClassifierName).jar ./$(artifact_name).zip
	@echo "Finished package"

.PHONY: dist
dist: clean build package
