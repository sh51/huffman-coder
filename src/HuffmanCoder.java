import java.io.*;
import java.util.*;

/**
 * Compress a file that:
 *      a) contains ascii characters only
 *      b) smaller than ~4GB
 * using Huffman coding
 *
 * Decompress a file created by the same class
 *
 * @author Sihao Huang
 *
 */

public class HuffmanCoder {
    static int SIZE = 256;  // size of the frequency map

    /**
     * a class that holds a pair of value and bit code (string)
     */
    static class Code {
        byte val;
        String code;
        public Code(byte v, String c) {
            val = v;
            code = c;
        }
        @Override
        public String toString() {
            return val + ": " + code + "\n";
        }
    }
    /**
     * Huffman tree
     * each leaf node holds an actual char (byte value)
     */
    static class Tree {
        static final byte NON_LEAF = -128; // value of non-leaf nodes
        static int curr;    // current index for deserializing a tree
        byte val;    // value of this node if it's a leaf (0-127)
        int f;      // frequency of this character
        Tree l, r;  // left right child of this node


        /**
         * Instantiate a Tree with a value-frequency pair, or two trees
         */
        public Tree(int val, int f) {
            this.val = (byte) val;
            this.f = f;
        }
        public Tree(Tree t1, Tree t2) {
            val = NON_LEAF;
            f = t1.f + t2.f;
            l = t1;
            r = t2;
        }

        /**
         *  Recursive toString() function for tree visualizaiton
         */
        @Override
        public String toString() {
            return toString(0);
        }
        public String toString(int level) {
            String s = indentString((val == NON_LEAF ? "-" : val) + ": " + f + "\n", level);
            s += l == null ? indentString("null\n", level + 1) : l.toString(level + 1);
            s += r == null ? indentString("null\n", level + 1) : r.toString(level + 1);
            return s;
        }
        /**
         * A shorthand for combining two trees
         */
        public Tree merge(Tree t) {
            return new Tree(this, t);
        }

        /**
         * convert a tree to a list of bytes for easy writing
         * @param bytes an empty byte list where the converted tree would be stored
         */
        public void serialize(List<Byte> bytes) {
            bytes.add(val);
            // huffman tree nodes either have no children, or have both
            if (l != null) {
                l.serialize(bytes);
                r.serialize(bytes);
            }
        }
        /**
         * reconstruct a tree from a list of bytes
         * the static variable Tree.curr needs to be reset to 0 before calling
         * @param bytes a byte list that contains a converted tree
         */
        public static Tree deserialize(List<Byte>bytes) {
            if (bytes.get(curr) == NON_LEAF) {
                curr++;
                return deserialize(bytes).merge(deserialize(bytes));
            } else return new Tree(bytes.get(curr++), 0);
        }
        /**
         * Get the codebook (map) represented by this Huffman tree for encoding
         */
        public Map<Byte, String> getCodebook() {
            Map<Byte, String> cb = new HashMap<>();
            List<Code> codes = new ArrayList<>();

            getCodes(this, codes, "");

            for (Code c: codes) cb.put(c.val, c.code);

            return cb;
        }
        /**
         * Recursive function to put all codes of the leaf nodes into a list
         */
        private void getCodes(Tree t, List<Code> codes, String suffix) {
            if (t == null) return;
            if (t.val == NON_LEAF) {
                getCodes(t.l, codes, suffix + "0");
                getCodes(t.r, codes, suffix + "1");
            } else {
                codes.add(new Code(t.val, suffix));
            }
        }
    }
    /**
     * Test function for Huffman Tree implementation
     */
    public static void testTree() {
        Tree t1 = new Tree(65, 65);
        Tree t2 = new Tree(66, 66);
        Tree t3 = new Tree(67, 67);
        Tree t4 = new Tree(68, 68);

        Tree t = t1.merge(t2).merge(t3).merge(t4);

        System.out.println("Tree: ABCD");
        System.out.println(t);

        System.out.println("Codebook:");
        Map<Byte, String> cb = t.getCodebook();
        cb.forEach((k, v) -> System.out.println(indentString(k + ": " + v, 1)));
    }
    /**
     * Test function for encode()
     * inputs/sample.txt would be compressed and saved to inputs/sample.shz
     */
    public static void testEncode() {
        String inputPath = "inputs/sample.txt";
        System.out.println("Test - encode");
        System.out.println("Compressing: " + inputPath);
        try {
            encode(inputPath);
        } catch (Exception e) {
            System.out.println("Compression failed.");
            System.out.println(e);
        }
        System.out.println("Saved to " + toOutputPath(inputPath));

    }
    /**
     * Test function for decode()
     * inputs/sample.shz would be decompressed and saved to inputs/sample_recovered.txt
     */
    public static void testDecode() {
        String inputPath = "inputs/sample.shz";
        System.out.println("Test - decode");
        System.out.println("Decompressing: " + inputPath);
        try {
            decode(inputPath);
        } catch (Exception e) {
            System.out.println("Decompression failed.");
            System.out.println(e);
        }
        System.out.println("Saved to " + toRecoveredPath(inputPath));
    }
    /**
     * Test function for encoding and decoding of an actual DNA sequence
     */
    public static void testDNA() throws Exception {
        encode("inputs/JWB-snps-submission.txt");
        System.out.println("DNA sequence file encoed.");
        decode("inputs/JWB-snps-submission.shz");
        System.out.println("DNA sequence file decoded.");
    }
    /**
     * Test function for encoding and decoding of an empty file
     */
    public static void testEmpty() throws Exception {
        encode("inputs/empty.txt");
        System.out.println("Empty file encoed.");
        decode("inputs/empty.shz");
        System.out.println("Empty file decoded.");
    }

    /**
     * encode a given file, and save it to the same directory.
     * suffix would be changed to .shz
     *
     * the compressed file contains all the information needed to decompress
     *
     * @param inputPath the filename of the compressed file
     */
    public static void encode(String inputPath) throws Exception {
        //  setup frequency array/map and instream
        int[] freq = new int[SIZE];
        BufferedReader input = new BufferedReader(new FileReader(inputPath));

        // calculate the frequency of each character
        int c;
        while ((c = input.read()) != -1) freq[c]++;

        // close the stream when done
        input.close();

        // setup a priority queue
        PriorityQueue<Tree> q = new PriorityQueue<>(1, Comparator.comparingInt(t -> t.f));
        // build a huffman tree
        for (int i = 0; i < SIZE; i++)
            if (freq[i] != 0) q.add(new Tree(i, freq[i]));
        while (q.size() > 1) q.add(q.poll().merge(q.poll()));
        Tree huffTree = q.poll();

        // an empty file, create an empty tree
        if (huffTree == null) huffTree = new Tree(1, 0);
        // get the codebook of this huffman tree for encoding
        Map<Byte, String> cb = huffTree.getCodebook();

        // generate the filename of the compressed file
        String outPath = toOutputPath(inputPath);
        // open iostreams for encoding
        BufferedBitWriter out = new BufferedBitWriter(outPath);
        input = new BufferedReader(new FileReader(inputPath));

        // first save the tree
        List<Byte> li = new ArrayList<>();
        huffTree.serialize(li);
        writeByte(out, (byte)li.size());
        for (Byte aByte : li) writeByte(out, aByte);
        // start writing data
        while ((c = input.read()) != -1) {
            String code = cb.get((byte)c);
            for (int i = 0; i < code.length(); i++) out.writeBit(code.charAt(i) != '0');
        }
        // close the streams when done
        input.close();
        out.close();
    }

    /**
     * decode a given file, and save it to the same directory.
     * suffix would be changed to _recovered.txt
     *
     * the Huffman tree would be reconstructed from the file header
     * and used to decode the rest of the file
     *
     * @param inputPath the filename of the compressed file
     */
    public static void decode(String inputPath) throws Exception {
        BufferedBitReader in = new BufferedBitReader(inputPath);
        BufferedWriter output = new BufferedWriter(new FileWriter(toRecoveredPath(inputPath)));

        int treeSize;
        List<Byte> treeBytes;
        Tree huffTree, decoder;
        // load the tree from the header
        try {
            treeSize = Byte.toUnsignedInt(readByte(in));
            treeBytes = new ArrayList<>();
            for (int i = 0; i < treeSize; i++) treeBytes.add(readByte(in));
            Tree.curr = 0;
            huffTree = Tree.deserialize(treeBytes);
        } catch (Exception e) {
            throw new Exception("Unable to load frequency tree.");
        }
        // start decoding the data
        decoder = huffTree;
        while ((decoder.val == Tree.NON_LEAF) && in.hasNext()) {
            decoder = in.readBit() ? decoder.r : decoder.l;
            if (decoder.val != Tree.NON_LEAF) {
                output.write(decoder.val);
                decoder = huffTree;
            }
        }

        in.close();
        output.close();
    }

    /**
     * Utility function: write a byte to a BufferedBitWriter
     *
     * @param out the output stream
     * @param b the byte to write
     */
    private static void writeByte(BufferedBitWriter out, Byte b) throws IOException {
        for (int i = 0; i < 8; i++) out.writeBit((b & (1 << 7 - i)) > 0);
    }
    /**
     * Utility function: read a byte from a BufferedBitReader
     *
     * @param in the input stream
     */
    private static Byte readByte(BufferedBitReader in) throws IOException {
        byte b = 0;
        for (int i = 0; i < 8; i++) b += (in.readBit() ? 1 : 0) << 7 - i;
        return b;
    }
    /**
     * Utility function: change the suffix of a path to .shz
     */
    private static String toOutputPath(String path) {
        for (int i = path.length() - 1; i >= 0; i--)
            if (path.charAt(i) == '.') return path.substring(0, i) + ".shz";
        return null;
    }
    /**
     * Utility function: change the suffix of a path to _recovered.txt
     */
    private static String toRecoveredPath(String path) {
        for (int i = path.length() - 1; i >= 0; i--)
            if (path.charAt(i) == '.') return path.substring(0, i) + "_recovered.txt";
        return null;
    }
    /**
     * Utility function: prefix tabs to a given string for pretty printing
     */
    static String indentString(String str, int indent) {
        String s = "";
        for (int i = 0; i < indent; i++) s += '\t';
        return s + str;
    }
}