# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.
raise "Remove patch in released" unless Buildr::VERSION == '1.4.10'

module Buildr
  class TestNG
    def run(tests, dependencies) #:nodoc:
      cmd_args = []
      cmd_args << '-suitename' << task.project.id
      cmd_args << '-sourcedir' << task.compile.sources.join(';') if TestNG.version < "6.0"
      cmd_args << '-log' << '2'
      cmd_args << '-d' << task.report_to.to_s
      exclude_args = options[:excludegroups] || []
      if !exclude_args.empty?
        cmd_args << '-excludegroups' << exclude_args.join(",")
      end
      groups_args = options[:groups] || []
      if !groups_args.empty?
        cmd_args << '-groups' << groups_args.join(",")
      end
      # run all tests in the same suite
      cmd_args << '-testclass' << (TestNG.version < "6.0" ? test : tests.join(','))

      cmd_args += options[:args] if options[:args]

      cmd_options = { :properties=>options[:properties], :java_args=>options[:java_args],
        :classpath=>dependencies, :name => "TestNG in #{task.send(:project).name}" }

      tmp = nil
      begin
        puts cmd_args.join("\n")
        tmp = Tempfile.open("testNG")
        tmp.write cmd_args.join("\n")
        tmp.close
        Java::Commands.java ['org.testng.TestNG', "@#{tmp.path}"], cmd_options
        return tests
      rescue
        # testng-failed.xml contains the list of failed tests *only*
        report = File.read(File.join(task.report_to.to_s, 'testng-failed.xml'))
        failed = report.scan(/<class name="(.*?)">/im).flatten
        error "TestNG regexp returned unexpected failed tests #{failed.inspect}" unless (failed - tests).empty?
        # return the list of passed tests
        return tests - failed
      ensure
        tmp.close unless tmp.nil?
      end
    end

  end
end
