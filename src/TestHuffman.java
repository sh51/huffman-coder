/**
 * Test program for HuffmanCoder
 *
 * @author Sihao Huang
 *
 */

public class TestHuffman {
    public static void main(String[] args) {
        try {

            // output a HuffmanTree and its corresponding codebook
            HuffmanCoder.testTree();
            // compress inputs/sample.txt
            HuffmanCoder.testEncode();
            // decompress inputs/sample.shz
            HuffmanCoder.testDecode();
            // encoding/decoding USConstitution.txt
            HuffmanCoder.encode("inputs/USConstitution.txt");
            HuffmanCoder.decode("inputs/USConstitution.shz");

//            // compress and decompress a file that contains James Watson's genome
//            HuffmanCoder.testDNA();
            // compress and decompress an empty file
            HuffmanCoder.testEmpty();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
