artifact_name       := search-service-comparison-utility
version             := "unversioned"

.PHONY: all
all: clean build

.PHONY: clean
clean:
	mvn clean
	rm -f ./$(artifact_name).jar
	rm -f ./$(artifact_name)-*.zip
	rm -rf ./build-*
	rm -rf ./build.log-*

.PHONY: build
build:
	mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	mvn package -DskipTests=true
	cp ./target/$(artifact_name)-$(version).jar ./$(artifact_name).jar

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
	@test -s ./$(artifact_name).jar || { echo "ERROR: Service JAR not found"; exit 1; }
	cp ./$(artifact_name).jar ./$(artifact_name).zip
	cp ./$(artifact_name)-$(version).jar ./$(artifact_name)-$(version).zip


.PHONY: dist
dist: clean build package
