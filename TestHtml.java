public class TestHtml {
    public static void main(String[] args) {
        String html = "<p style=\"text-align: center;\"><img alt=\"thiết kế cứng cáp\" src=\"//cdn.hstatic.net/files/2000/1.jpg\"/></p>";
        System.out.println("Original: " + html);
        html = html.replace("src=\"//", "src=\"https://");
        System.out.println("Replaced: " + html);
    }
}
