# Dependency Manager gradle plugin
## Gradle plugin to manage and download dependencies.

### Usage

1. Configure the dependencies types on the configuration block

```groovy
configurations {
    base
    cspDepenency
    install4jDependencies
    java
}
```

2. Declare your dependencies
    
    a. You can specify the artifact type with the `ext` argument and provide several types (exe, zip, jar, tgz)

    b. by using the `because` argument you can require to decompress the file nd specify a location, i.e. `because:decompress=.;target=build`

```groovy
dependencies{
    java group: 'com.roche.modules.eppr', name: 'eppr-runtime', version: '2.0.0-SNAPSHOT', classifier: 'installer', ext: 'exe'
    java group: 'com.roche.modules.qc', name: 'qc-core-runtime', version: '3.0.0.1-SNAPSHOT', classifier: 'installer', ext: 'exe'
    java group: 'com.roche.sis.infinity', name: 'infinity-lis-gen-reports', version: '0.0.13', ext: 'zip', because: "decompress=.;target=${csp_acb_lisPages_folder}"
    java group: 'com.roche.sis.infinity', name: 'infinity-reports-server', version: '0.0.2', ext: 'zip', because: "decompress=.;target=${csp_acb_lisPages_folder}"
    cspDependency group: 'com.roche.modules', name: 'igen', version: '2.0.0-SNAPSHOT', classifier: 'installer', ext: 'exe', because: "target=."
    cspDependency group: 'com.roche.infinity', name: 'infinity-configurator', version: '2.0.0-ALPHA24', ext: 'zip', because: "decompress=.;target=toparse"
    cspDependency group: 'npm', name: 'infinity-ui-workarea-charts', version: '0.0.+', ext: 'tgz', because: "asdf"
    cspDependency group: 'npm', name: 'infinity-ui-serviceability-hosts-wizard', version: '2.0.+', ext: 'tgz', because: "decompress=."
    cspDependency group: 'npm', name: 'infinity-ui-serviceability-tunetable', version: '1.2.+', ext: 'tgz', because: "target=csp/serviceability-tunetable"
    cspDependency group: 'npm', name: 'infinity-ui-ruleengine-authoring', version: '1.0.+', ext: 'tgz', because: "target=csp/ruleengine/unzip;decompress=."
    cspDependency group: 'roche', name: 'components-library-ui', version: '1.1.+', ext: 'tgz', because: "target=csp/components/unzipPackage;decompress=package"
    cspDependency group: 'com.roche.sis.infinity', name: 'infinity-lis-gen-reports', version: '0.0.13', ext: 'zip', because: "decompress=.;target=csp/acb/LISPages"
    install4jDependencies(group: 'com.ej-technologies', name: 'adoptopenjdk', version: "11.0.7_10", classifier: 'jre_x64_windows_hotspot', ext: 'tar.gz', because: "target=jre")
    install4jDependencies group: 'com.roche.installer.install4j', name: 'install4j-java-library', version: '1.0.6'
    install4jDependencies group: 'com.roche.infinity.installer.install4j', name: 'healthshare-actions', version: "1.0.0-ALPHA7"
}
```

3. Apply the plugin

```groovy
apply plugin: 'org.devopsmindset.dependency-manager'
```

4. Tune up de plugin configuration with the `dpendenciesManagement` block
    
    a. Use the `configurations` block to declare the different dependency types.

    b. Indicate the `stripVersion` to delete the version on the downloaded file.

    c. Use the `separateByGroupId` whether to download same group artifacts in the same folder.

```groovy
dependenciesManagement {
    configurations = [['cspDependency'], ['install4jDependencies']]
    stripVersion = [true, true]
    separateByGroupId = [true, false]
}
```