require 'buildr/git_auto_version'
require 'buildr/bnd'
require 'buildr/gpg'

desc 'GELF4j: Library for sending log messages using the GELF protocol'
define 'gelf4j' do
  project.group = 'org.realityforge.gelf4j'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/gelf4j')
  pom.add_developer('realityforge', 'Peter Donald', 'peter@realityforge.org', ['Developer'])
  pom.add_developer('Moocar', 'Anthony Marcar', 'Anthony.Marcar@gmail.com', ['Developer'])
  pom.add_developer('t0xa', 'Anton Yakimov', 'anton.jakimov@gmail.com', ['Developer'])
  pom.add_developer('joschi', 'Jochen Schalanda', 'jochen@schalanda.name', ['Developer'])
  pom.optional_dependencies.concat [:getopt4j, :slf4j_api, :log4j, :logback_core, :logback_classic]

  compile.with :getopt4j, :json_simple, :slf4j_api, :log4j, :logback_core, :logback_classic
  test.using :junit

  package(:bundle).tap do |bnd|
    bnd['Private-Package'] = 'gelf4j.sender.*'
    bnd['Import-Package'] = '!org.realityforge.getopt4j.*, *;resolution:=optional'
    bnd['Export-Package'] = "gelf4j.*;version=#{version}"
  end
  package(:bundle, :classifier => 'all').tap do |bnd|
    bnd['Main-Class'] = 'gelf4j.sender.Main'
    bnd['Bundle-SymbolicName'] = 'org.realityforge.gelf4j-all'
    bnd['Private-Package'] = 'org.realityforge.getopt4j.*,org.json.simple.*'
    bnd['Export-Package'] = "gelf4j.*;version=#{version}"
  end
  package(:sources)
  package(:javadoc)
end
