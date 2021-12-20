# This is a useful small shell script to automatically copy the Java Keystores (.jks files), .p12 containers and the DER encoded Mobility Operator Sub-CA private key to the places in the RISE V2G project where they belong. Execute this script after you executed the generateCertificates.sh script.

cp keystores/evccKeystore.jks ../
cp keystores/evccTruststore.jks ../
cp keystores/seccKeystore.jks ../
cp keystores/seccTruststore.jks ../

cp certs/cpsCertChain.p12 ../
cp certs/moCertChain.p12 ../

cp privateKeys/moSubCA2.pkcs8.der ../
