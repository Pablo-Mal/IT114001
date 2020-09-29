public class ConvertRecursion {
  
	public static int sum(int num) {
		if (num > 0) {
			return num + sum(num - 1);
		}
		return 0;
	}
   
   public static int sumCopy(int numCopy) {
      int num2 = 0;
      while ( numCopy > 0){
         num2 += numCopy;
         numCopy--;
      }
      return num2;
   }

	public static void main(String[] args) {
		System.out.println(sum(10));
      System.out.println(sumCopy(10)); 
   }
   
}