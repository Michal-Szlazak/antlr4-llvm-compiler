fun fun1 {
    write "hello from function1";
};

fun fun2 {

    i32 x;
    i32 y;

    x = 2;
    y = 3;
    write "x + y (x + y)";
    write x + y;
};

call fun1;
call fun2;
call fun1;