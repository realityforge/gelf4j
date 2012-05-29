require "buildr/bnd"

desc "GELF4j: Library for sending log messages using the GELF protocol"
define "gelf4j" do
  project.version = `git describe --tags --always`.strip
  project.group = "gelf4j"
  compile.options.source = '1.6'
  compile.options.target = '1.6'
  compile.options.lint = 'all'

  compile.with :spice_cli, :json_simple, :slf4j_api, :log4j, :logback_core, :logback_classic

  package(:bundle).tap do |bnd|
    bnd['Private-Package'] = "gelf4j.sender.*"
    bnd['Import-Package'] = "!org.realityforge.cli.*, *;resolution:=optional"
    bnd['Export-Package'] = "gelf4j.*;version=#{version}"
  end
  package(:bundle, :classifier => 'all').tap do |bnd|
    bnd['Bundle-SymbolicName'] = "gelf4j.gelf4j-all"
    bnd['Private-Package'] = "org.realityforge.cli.*,org.json.simple.*"
    bnd['Export-Package'] = "gelf4j.*;version=#{version}"
  end
end
