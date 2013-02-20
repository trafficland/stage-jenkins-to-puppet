#!/bin/sh
pathToGenericModule=${1?missing path to generic module}
moduleName=${2?missing moduleName}
emails=${3?missing email or emails in quotes}

cp -R $pathToGenericModule ./"$moduleName"

find ./"$moduleName" -type f|xargs perl -pi -e 's/REPLACE_MODULE_NAME/'"$moduleName"'/g'

moduleNameCAPS=$(echo "$moduleName" | tr '[:lower:]' '[:upper:]')
find ./"$moduleName" -type f|xargs perl -pi -e 's/REPLACE_CAPS_MODULE_NAME/'"$moduleNameCAPS"'/g'

find ./"$moduleName" -type f|xargs perl -pi -e 's/dummy\@gmail.com/'"$emails"'/g'

mv ./"$moduleName"'/files/bin/module' ./"$moduleName"'/files/bin/'"$moduleName"''