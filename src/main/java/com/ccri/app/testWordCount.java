package com.ccri.app;

import cascading.flow.Flow;
import cascading.flow.FlowConnector;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.operation.Aggregator;
import cascading.operation.aggregator.Count;
import cascading.operation.regex.RegexGenerator;
import cascading.pipe.Each;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.property.AppProps;
import cascading.scheme.Scheme;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.SinkMode;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;
import cascading.operation.Function;
import sun.tools.jar.Main;

import java.util.Properties;

/**
 * Created by jw9bn on 12/29/14.
 */
public class testWordCount {
    public void testWordCount(String inputPath, String outputPath) {
        // define source and sink Taps.
        Scheme sourceScheme = new TextLine( new Fields( "line" ) );
        Tap source = new Hfs( sourceScheme, inputPath );

        Scheme sinkScheme = new TextLine( new Fields( "word", "count" ) );
        Tap sink = new Hfs( sinkScheme, outputPath, SinkMode.REPLACE );

        // the 'head' of the pipe assembly
        Pipe assembly = new Pipe( "wordcount" );

        // For each input Tuple
        // parse out each word into a new Tuple with the field name "word"
        // regular expressions are optional in Cascading
        String regex = "(?<!\\pL)(?=\\pL)[^ ]*(?<=\\pL)(?!\\pL)";
        Function function = new RegexGenerator( new Fields( "word" ), regex );
        assembly = new Each( assembly, new Fields( "line" ), function );

        // group the Tuple stream by the "word" value
        assembly = new GroupBy( assembly, new Fields( "word" ) );

        // For every Tuple group
        // count the number of occurrences of "word" and store result in
        // a field named "count"
        Aggregator count = new Count( new Fields( "count" ) );
        assembly = new Every( assembly, count );

        // initialize app properties, tell Hadoop which jar file to use
        Properties properties = new Properties();
        AppProps.setApplicationJarClass(properties, Main.class);

        // plan a new Flow from the assembly using the source and sink Taps
        // with the above properties
//        FlowConnector flowConnector = new HadoopFlowConnector( properties );
        FlowConnector flowConnector = new HadoopFlowConnector();
        Flow flow = flowConnector.connect( "word-count", source, sink, assembly );

        // execute the flow, block until complete
        flow.complete();
    }


    public static void main(String[] args) {

        testWordCount twc = new testWordCount();
        twc.testWordCount("/tmp/hello.txt", "/tmp/hello_out.txt");
    }
}
