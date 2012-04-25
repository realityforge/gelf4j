desc "GELF4j: Library for sending log messages using the GELF protocol"
define "gelf4j" do
  project.version = `git describe --tags --always`.strip
  project.group = "gelf4j"
  compile.options.source = '1.6'
  compile.options.target = '1.6'
  compile.options.lint = 'all'

  compile.with :slf4j_api, :spice_cli, :json_simple, :log4j, :logback_core, :logback_classic

  package(:jar)
  package(:jar, :classifier => 'all').merge(artifacts([:spice_cli,:json_simple]))
end
