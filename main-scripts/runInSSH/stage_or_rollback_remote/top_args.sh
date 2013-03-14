applicationName=${1?missing application name}

stagePath=${2?missing stage path}

extension=${3?missing extension}

destinationAddress=${4?missing destination address}

extractCmd=${5?missing extraction command like "unzip"}

renameApplicationTo=${6:-$applicationName}

startName=${7:-}
