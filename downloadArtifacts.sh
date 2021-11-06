rm -f "${MD_APP}_${MD_ENV} .${MD_APP}_${MD_ENV}"
wget http://azcuvctbartft01.activehealth.loc:8081/artifactory/api/storage/libs-release-local/package/SDLC/IIB/${MD_APP}/${MD_ENV}?lastModified -O ${MD_APP}_${MD_ENV}

VAR=`cat ${MD_APP}_${MD_ENV} | grep uri | awk -F/ '{ print $(NF - 1) }'`

echo "VAR=${VAR}" > ${MD_APP}_${MD_ENV}.properties
