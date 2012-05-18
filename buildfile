desc "GELF4j: Library for sending log messages using the GELF protocol"
define "gelf4j" do
  project.version = `git describe --tags --always`.strip
  project.group = "gelf4j"
  compile.options.source = '1.6'
  compile.options.target = '1.6'
  compile.options.lint = 'all'

  compile.with :getopt4j, :json_simple, :slf4j_api, :log4j, :logback_core, :logback_classic

  package(:jar)
  package(:jar, :classifier => 'all').merge(artifacts([:getopt4j,:json_simple]))
end
