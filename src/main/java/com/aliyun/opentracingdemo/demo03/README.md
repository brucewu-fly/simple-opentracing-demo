# Demo 3 - 异常处理

## 目标
* 了解如果待追踪的方法在执行过程中抛出异常该如何处理
* 不同创建 span 的方法在遇到未捕获异常时所表现出的行为

## 样例说明
下列方法在执行过程中会抛出异常，且异常未被捕获。
```
private static void func(boolean invokeException) {
	System.out.println("in func1");
	if (invokeException) {
	  	throw new RuntimeException("func1 RuntimeException");
	}
}
```

## 方式一

Span 的 `finish()` 方法不会被调用，该 span 不会被记录。
```
private static void func1(boolean invokeException) {
	Span span = TracerHelper.buildSpan("func1").start();

	System.out.println("in func1");
	if (invokeException) {
		throw new RuntimeException("func1 RuntimeException");
	}

	span.finish();
}
```

## 方式二
Span 的 `finish()` 方法在 finally 中被调用，该 span 会被记录。
```
private static void func2(boolean invokeException) {
	Span span = TracerHelper.buildSpan("func2").start();

	try {
		System.out.println("in func2");
	  	if (invokeException) {
	    	throw new RuntimeException("func2 RuntimeException");
	  	}
	} finally {
	  	span.finish();
	}
}
```

## 方式三

Span 的 `finish()` 方法在当前 Scope 被自动 close 时调用，该 span 会被记录。
```
private static void func3(boolean invokeException) {
	try (Scope scope =TracerHelper.buildSpan("func3").startActive(true)) {
		System.out.println("in func3");
		if (invokeException) {
			throw new RuntimeException("func3 RuntimeException");
		}
	}
}
```

## 方式四
由于 startActive 的布尔参数被设成了 false，因此当前 Scope 被自动 close 时，不会调用 span 的 finish 方法，该 span 不会被记录。
```
private static void func4(boolean invokeException) {
	try (Scope scope = TracerHelper.buildSpan("func4").startActive(false)) {
		System.out.println("in func4");
		if (invokeException) {
			throw new RuntimeException("func4 RuntimeException");
		}
	}
}
```

## 方式五
对抛出地异常进行捕获，并为 span 增加一些 tag。该 span 会被记录，并且包含 error: true 这个 tag。
```
private static void func5(boolean invokeException) {
	Scope scope = TracerHelper.buildSpan("func5").startActive(true);

	try {
		System.out.println("in func5");
		if (invokeException) {
			throw new RuntimeException("func5 RuntimeException");
		}
	} catch (Throwable ex) {
	  	scope.span().setTag("error", true);
	} finally {
	  	scope.close();
	}
}
```

## 方式六
```
private static void func6(boolean invokeException) {
  Scope scope = TracerHelper.traceLatency("func6");

  try {
    System.out.println("in func6");
    if (invokeException) {
      throw new RuntimeException("func6 RuntimeException");
    }
  } catch (Throwable ex) {
    scope.span().setTag("error", true);
  } finally {
    scope.close();
  }
}
```

## 打印堆栈信息
```
private static void func7() {
  try (Scope scope = TracerHelper.buildSpan("func7").startActive(true)) {
    System.out.println("in func7");
    try {
      f1();
    } catch (Throwable ex) {
      TracerHelper.logThrowable(scope.span(), ex);
    }
  }
}

private static void f1() {
  f2();
}

private static void f2() {
  f3();
}

private static void f3() {
  f4();
}

private static void f4() {
  int a = 10 / 0;
}
```
