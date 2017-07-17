#!/bin/bash
echo -e "Deploying version info gist!\n"
cd $TRAVIS_BUILD_DIR
sudo apt-get -qq update
sudo apt-get install -y jq
curl -H "Authorization: token $GITPERM" https://api.github.com/users/PHENOMICAL -I
export GIST_URL=$(curl -H "Content-Type: application/json" -X POST -d "{\"description\":\"Created through Travis-CI\",\"public\":\"false\",\"files\":{\"version.yml\":{\"content\":\"Branch: ${TRAVIS_BRANCH}\nBuild: ${TRAVIS_BUILD_NUMBER}\nVersion: ${TARGET_VERSION}\"}}}" https://api.github.com/gists | jq  '.files."version.yml".raw_url')
echo -e "Gist successfully deployed! URL: $GIST_URL\n"
echo -e "Finished deploying version info!\n"