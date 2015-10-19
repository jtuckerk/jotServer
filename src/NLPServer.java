import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;

/**
 * Created by tuckerkirven on 9/25/15.
 */
public class NLPServer {

    static StanfordCoreNLP pipeline;
    static Annotation annotation;

    public  NLPServer(){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, sentiment, relation, natlog, entitymentions");
        pipeline = new StanfordCoreNLP(props);
    }

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();

        //not all of these components may be necessary
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, sentiment, natlog");
        pipeline = new StanfordCoreNLP(props);

        System.out.println("starting server");
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/entry", new entryHandler());
        server.createContext("/feedback", new feedbackHandler());

        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class entryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();

            String newEntry = convertStreamToString(is);
            annotation = new Annotation(newEntry);

            File yourFile = new File("processed_entries.txt");
            if(!yourFile.exists()) {
                yourFile.createNewFile();
            }
            FileOutputStream oFile = new FileOutputStream(yourFile, true);
            oFile.write(newEntry.getBytes());
            oFile.write("\n\n".getBytes());

            pipeline.annotate(annotation);

            // An Annotation is a Map and you can get and use the various analyses individually.
            // For instance, this gets the parse tree of the first sentence in the text.
            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

            if (sentences != null && sentences.size() > 0) {
                CoreMap sentence = sentences.get(0);
                Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);

            }

            //XML http response
            OutputStream xmlOut = new ByteArrayOutputStream();
            pipeline.xmlPrint(annotation, xmlOut);
            String response = "" + xmlOut.toString();
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        static String convertStreamToString(java.io.InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    static class feedbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();

            PrintWriter out = new PrintWriter(System.out);

            File yourFile = new File("feedback.txt");
            if(!yourFile.exists()) {
                yourFile.createNewFile();
            }
            FileOutputStream oFile = new FileOutputStream(yourFile, true);

            oFile.write(convertStreamToString(is).getBytes());
            oFile.write("\n\n".getBytes());

            OutputStream os = t.getResponseBody();
            t.sendResponseHeaders( 200, 0 );
            os.close();
        }

        static String convertStreamToString(java.io.InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }
}
