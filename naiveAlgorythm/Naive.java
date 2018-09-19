public class Naive{
    public static void main(String[] args) {
        String s ="dupajasdzialaczynie";
        String p = "jas";
        int n = s.length();
        int m = p.length();
        int j = 0;
        int i = 0;
        for (i = 0; i<n-m; i++) {
            for (j = 0; j < m; j++) {
                if (s.charAt(i+j)==p.charAt(j)){
                }
                else {
                    break;
                }
            }
            if(j==m){
                System.out.println("Pattern found at index" + i);
            }
                    

        }
    }
}