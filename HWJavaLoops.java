import java.util.Arrays;
public class HWJavaLoops
{
   public static void main(String[] args)
   {
      //#1
      int[] arr = new int[] {1,2,3,4};
      
      //#2
      System.out.println("-----#2-----");
      for(int num : arr)
      {
         System.out.println(num);
      }
      
      //#3
      System.out.println("-----#3-----");
      for(int num : arr)
      {
         if(num%2 == 0)
         {
            System.out.println(num);
         }
      }
   }
}