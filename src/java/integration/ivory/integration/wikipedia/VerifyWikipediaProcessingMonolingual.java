package ivory.integration.wikipedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import ivory.core.data.document.WeightedIntDocVector;
import ivory.core.driver.PreprocessWikipedia;
import ivory.integration.IntegrationUtils;

import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.JUnit4TestAdapter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import edu.umd.cloud9.io.map.HMapSFW;

public class VerifyWikipediaProcessingMonolingual {
  private static final Random rand = new Random();
  private static final String tmp = "tmp-" + VerifyWikipediaProcessingMonolingual.class.getSimpleName() + rand.nextInt(10000);

  private static final String collectionPath = 
    "/shared/collections/wikipedia/raw/enwiki-20110115-pages-articles.xml";
  private static final String collectionRepacked = tmp + "/enwiki-20110115.repacked";
  private static final String galagoIndex = tmp + "/enwiki.galago";

  // Galago: part 00000, key = 92101
  private ImmutableMap<String, Float> galagoTermDocVector1 = ImmutableMap.of(
    "total", 0.036282938f, "posit", 0.047018476f, "valid", 0.07093949f, "formula", 0.06923077f);

  // Galago: part 00010, key = 34222
  private ImmutableMap<String, Float> galagoTermDocVector2 = ImmutableMap.of(
    "conjunto", 0.33945185f, "librari", 0.12812887f, "film", 0.10551942f, "cultur", 0.10863351f);

  // Galago: part 00002, key = 100984
  private ImmutableMap<Integer, Float> galagoIntDocVector1 =
    ImmutableMap.of(5, 0.017268969f, 1861, 0.08070707f, 7524, 0.11860653f, 31405, 0.16086219f);

  // Galago: part 00011, key = 138960
  private ImmutableMap<Integer, Float> galagoIntDocVector2 =
    ImmutableMap.of(2, 0.003051088f, 156, 0.03952723f, 2726, 0.08285294f, 402710, 0.20997283f);

  private static final String opennlpIndex = tmp + "/enwiki.opennlp";
  private static final String vocabPath = tmp + "/vocab";

  // Opennlp: part 00000, key = 92101
  private ImmutableMap<String, Float> opennlpTermDocVector1 = ImmutableMap.of(
    "external", 0.0026067079f, "zero", 0.05407416f, "theorem", 0.066354945f, "prime", 0.04295602f);

  // Opennlp: part 00010, key = 14178
  private ImmutableMap<String, Float> opennlpTermDocVector2 = ImmutableMap.of(
     "gyroscop", 0.039886165f, "hors", 0.011038983f, "m551", 0.030033048f, "plastic", 0.013833861f);

  // Opennlp: part 00002, key = 100984
  private ImmutableMap<Integer, Float> opennlpIntDocVector1 =
    ImmutableMap.of(2193, 0.07499186f, 12, 0.018467898f, 3290, 0.08417095f, 15, 0.025152508f);

  // Opennlp: part 00011, key = 34222
  private ImmutableMap<Integer, Float> opennlpIntDocVector2 =
    ImmutableMap.of(15, 0.04056658f, 24, 0.05205808f, 31, 0.06348826f, 30, 0.057549093f);

  @Test
  public void runBuildIndexGalago() throws Exception {
    Configuration conf = IntegrationUtils.getBespinConfiguration();
    FileSystem fs = FileSystem.get(conf);

    assertTrue(fs.exists(new Path(collectionPath)));

    fs.delete(new Path(galagoIndex), true);
    fs.delete(new Path(collectionRepacked), true);

    List<String> jars = Lists.newArrayList();
    jars.add(IntegrationUtils.getJar("lib", "cloud9"));
    jars.add(IntegrationUtils.getJar("lib", "bliki-core"));
    jars.add(IntegrationUtils.getJar("lib", "guava"));
    jars.add(IntegrationUtils.getJar("lib", "dsiutils"));
    jars.add(IntegrationUtils.getJar("lib", "fastutil"));
    jars.add(IntegrationUtils.getJar("lib", "jsap"));
    jars.add(IntegrationUtils.getJar("lib", "sux4j"));
    jars.add(IntegrationUtils.getJar("lib", "commons-collections"));
    jars.add(IntegrationUtils.getJar("lib", "commons-lang"));
    jars.add(IntegrationUtils.getJar("lib", "tools"));
    jars.add(IntegrationUtils.getJar("lib", "maxent"));
    jars.add(IntegrationUtils.getJar("dist", "ivory"));

    String libjars = String.format("-libjars=%s", Joiner.on(",").join(jars));

    PreprocessWikipedia.main(new String[] { libjars,
        IntegrationUtils.D_JT, IntegrationUtils.D_NN,
        galagoIndex, collectionPath, collectionRepacked,
        ivory.core.tokenize.GalagoTokenizer.class.getCanonicalName(), "en" });
  }

  @Test
  public void verifyTermDocVectorsGalago() throws Exception {
    Configuration conf = IntegrationUtils.getBespinConfiguration();
    FileSystem fs = FileSystem.get(conf);

    SequenceFile.Reader reader;
    IntWritable key = new IntWritable();
    HMapSFW value = new HMapSFW();

    reader = new SequenceFile.Reader(fs,
        new Path(galagoIndex + "/wt-term-doc-vectors/part-00000"), fs.getConf());
    reader.next(key, value);
    verifyTermDocVector(galagoTermDocVector1, value);

    reader = new SequenceFile.Reader(fs,
        new Path(galagoIndex + "/wt-term-doc-vectors/part-00010"), fs.getConf());
    reader.next(key, value);
    verifyTermDocVector(galagoTermDocVector2, value);
  }

  @Test
  public void verifyIntDocVectorsGalago() throws Exception {
    Configuration conf = IntegrationUtils.getBespinConfiguration();
    FileSystem fs = FileSystem.get(conf);

    SequenceFile.Reader reader;
    IntWritable key = new IntWritable();
    WeightedIntDocVector value = new WeightedIntDocVector();

    reader = new SequenceFile.Reader(fs,
        new Path(galagoIndex + "/wt-int-doc-vectors/part-00002"), fs.getConf());
    reader.next(key, value);
    verifyIntDocVector(galagoIntDocVector1, value);

    reader = new SequenceFile.Reader(fs,
        new Path(galagoIndex + "/wt-int-doc-vectors/part-00011"), fs.getConf());
    reader.next(key, value);
    verifyIntDocVector(galagoIntDocVector2, value);
  }

  @Test
  public void runBuildIndexOpennlp() throws Exception {
    Configuration conf = IntegrationUtils.getBespinConfiguration();
    FileSystem fs = FileSystem.get(conf);

    assertTrue(fs.exists(new Path(collectionPath)));

    fs.delete(new Path(opennlpIndex), true);
    fs.delete(new Path(collectionRepacked), true);
    fs.delete(new Path(vocabPath), true);

    fs.copyFromLocalFile(false, true, new Path("data/vocab"), new Path(vocabPath));

    List<String> jars = Lists.newArrayList();
    jars.add(IntegrationUtils.getJar("lib", "cloud9"));
    jars.add(IntegrationUtils.getJar("lib", "bliki-core"));
    jars.add(IntegrationUtils.getJar("lib", "guava"));
    jars.add(IntegrationUtils.getJar("lib", "dsiutils"));
    jars.add(IntegrationUtils.getJar("lib", "fastutil"));
    jars.add(IntegrationUtils.getJar("lib", "jsap"));
    jars.add(IntegrationUtils.getJar("lib", "sux4j"));
    jars.add(IntegrationUtils.getJar("lib", "commons-collections"));
    jars.add(IntegrationUtils.getJar("lib", "commons-lang"));
    jars.add(IntegrationUtils.getJar("lib", "tools"));
    jars.add(IntegrationUtils.getJar("lib", "maxent"));
    jars.add(IntegrationUtils.getJar("dist", "ivory"));

    String libjars = String.format("-libjars=%s", Joiner.on(",").join(jars));

    PreprocessWikipedia.main(new String[] { libjars,
        IntegrationUtils.D_JT, IntegrationUtils.D_NN,
        opennlpIndex, collectionPath, collectionRepacked,
        ivory.core.tokenize.OpenNLPTokenizer.class.getCanonicalName(), "en",
        vocabPath + "/en-token.bin"});
  }

  @Test
  public void verifyTermDocVectorsOpennlp() throws Exception {
    Configuration conf = IntegrationUtils.getBespinConfiguration();
    FileSystem fs = FileSystem.get(conf);

    SequenceFile.Reader reader;
    IntWritable key = new IntWritable();
    HMapSFW value = new HMapSFW();

    reader = new SequenceFile.Reader(fs,
        new Path(opennlpIndex + "/wt-term-doc-vectors/part-00000"), fs.getConf());
    reader.next(key, value);
    verifyTermDocVector(opennlpTermDocVector1, value);

    reader = new SequenceFile.Reader(fs,
        new Path(opennlpIndex + "/wt-term-doc-vectors/part-00010"), fs.getConf());
    reader.next(key, value);
    verifyTermDocVector(opennlpTermDocVector2, value);
  }

  @Test
  public void verifyIntDocVectorsOpennlp() throws Exception {
    Configuration conf = IntegrationUtils.getBespinConfiguration();
    FileSystem fs = FileSystem.get(conf);

    SequenceFile.Reader reader;
    IntWritable key = new IntWritable();
    WeightedIntDocVector value = new WeightedIntDocVector();

    reader = new SequenceFile.Reader(fs,
        new Path(opennlpIndex + "/wt-int-doc-vectors/part-00002"), fs.getConf());
    reader.next(key, value);
    verifyIntDocVector(opennlpIntDocVector1, value);

    reader = new SequenceFile.Reader(fs,
        new Path(opennlpIndex + "/wt-int-doc-vectors/part-00011"), fs.getConf());
    reader.next(key, value);
    verifyIntDocVector(opennlpIntDocVector2, value);
  }

  private void verifyTermDocVector(Map<String, Float> doc, HMapSFW value) {
    for (Map.Entry<String, Float> entry : doc.entrySet()) {
      assertTrue(value.containsKey(entry.getKey()));
      assertEquals(entry.getValue(), value.get(entry.getKey()), 10e-6);
    }
  }

  private void verifyIntDocVector(Map<Integer, Float> doc, WeightedIntDocVector value) {
    for (Map.Entry<Integer, Float> entry : doc.entrySet()) {
      assertTrue(value.containsTerm(entry.getKey()));
      assertEquals(entry.getValue(), value.getWeight(entry.getKey()), 10e-6);
    }
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(VerifyWikipediaProcessingMonolingual.class);
  }
}