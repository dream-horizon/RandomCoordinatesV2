#!/bin/bash
echo -e "Starting to tag commit.\n"
cd $TRAVIS_BUILD_DIR
git config --global user.email "builds@travis-ci.org"
git config --global user.name "Travis CI"
export GIT_TAG=build-$TRAVIS_BRANCH-$(date -u "+%Y-%m-%d-%H-%M-%S")-$TRAVIS_BUILD_NUMBER
git tag $GIT_TAG -a -m "Generated tag from TravisCI build $TRAVIS_BUILD_NUMBER"
git push https://$GITPERM@github.com/$TRAVIS_REPO_SLUG $GIT_TAG
git fetch origin
echo -e "Finished with tagging.\n"