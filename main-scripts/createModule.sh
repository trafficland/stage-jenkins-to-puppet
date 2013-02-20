#!/bin/sh
pathToGenericModule=${1?missing path to generic module}
moduleName=${2?missing moduleName}

cp -R $pathToGenericModule ./"$moduleName"

find "$moduleToModify" -type f -exec \
    sed -i 's/REPLACE_MODULE_NAME/$moduleName/g' {} +


#look for and replace all caps strings with MODULENAME
find "$moduleToModify" -type f -exec \
    sed -i 's/REPLACE_MODULE_NAME_CAPS/${moduleName^^}/g' {} +


mv ./"$moduleName"'/bin/module' ./"$moduleName"'/bin/"$moduleName"'