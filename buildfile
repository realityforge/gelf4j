require 'buildr/git_auto_version'
require 'buildr/bnd'
require 'buildr/gpg'

desc 'GELF4j: Library for sending log messages using the GELF protocol'
define 'gelf4j' do
  project.group = 'org.realityforge.gelf4j'
  compile.options.source = '1.6'
  compile.options.target = '1.6'
  compile.options.lint = 'all'

  compile.with :getopt4j, :json_simple, :slf4j_api, :log4j, :logback_core, :logback_classic

  package(:bundle).tap do |bnd|
    bnd['Private-Package'] = 'gelf4j.sender.*'
    bnd['Import-Package'] = '!org.realityforge.getopt4j.*, *;resolution:=optional'
    bnd['Export-Package'] = "gelf4j.*;version=#{version}"
  end
  package(:bundle, :classifier => 'all').tap do |bnd|
    bnd['Main-Class'] = 'gelf4j.sender.Main'
    bnd['Bundle-SymbolicName'] = 'gelf4j.gelf4j-all'
    bnd['Private-Package'] = 'org.realityforge.getopt4j.*,org.json.simple.*'
    bnd['Export-Package'] = "gelf4j.*;version=#{version}"
  end
end
