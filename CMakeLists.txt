# copy rise-v2g source tree to current bin dir and use maven to build it there
# make our module target depend on the maven jar output we are using in the install below
file(GLOB_RECURSE MAVEN_SOURCES RELATIVE ${CMAKE_CURRENT_SOURCE_DIR} "${CMAKE_CURRENT_SOURCE_DIR}/RISE-V2G-*/*")
list(FILTER MAVEN_SOURCES EXCLUDE REGEX "\.jar$")

set(RISE_V2G_JARS
    ${CMAKE_CURRENT_BINARY_DIR}/RISE-V2G/RISE-V2G-SECC/target/rise-v2g-secc-1.2.6.jar
    ${CMAKE_CURRENT_BINARY_DIR}/RISE-V2G/RISE-V2G-EVCC/target/rise-v2g-evcc-1.2.6.jar
)

add_custom_command(
    OUTPUT .risev2g_rsynced
    COMMENT "Copying RISE-V2G sources to build dir..."
    COMMAND rsync -avq --exclude="*/target" --delete "${CMAKE_CURRENT_SOURCE_DIR}/" "${CMAKE_CURRENT_BINARY_DIR}/RISE-V2G"
    COMMAND ${CMAKE_COMMAND} -E touch .risev2g_rsynced
    DEPENDS ${MAVEN_SOURCES}
)

add_custom_command(
    OUTPUT ${RISE_V2G_JARS}
    COMMENT "Building RISE-V2G using maven..."
    COMMAND mvn -q package
    DEPENDS .risev2g_rsynced
    WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/RISE-V2G/RISE-V2G-PARENT
)

add_custom_target(
    RISEV2G_BUILD ALL
    DEPENDS ${RISE_V2G_JARS}
)

# install the rise-v2g jars and tools
install(
    FILES ${RISE_V2G_JARS}
          ${CMAKE_CURRENT_SOURCE_DIR}/RISE-V2G-SECC/SECCConfig.properties
          ${CMAKE_CURRENT_SOURCE_DIR}/RISE-V2G-EVCC/EVCCConfig.properties
    DESTINATION "${THIRD_PARTY_APP_DST}/rise_v2g"
)

install(
    DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/RISE-V2G-Certificates
    DESTINATION "${THIRD_PARTY_APP_DST}/rise_v2g"
)
