
public class B extends A{
    public int x = 1;
    public B() {}
    public static int test() {
	B b = new B();
	A a = b;
	return a.x;

    }
}
