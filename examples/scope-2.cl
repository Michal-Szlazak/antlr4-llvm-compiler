
i32 y;
y = 100;

fun fun1 {
    i32 y;
    y = 10;
    call fun2;
    write y;
};

fun fun2 {
    i32 y;
    y = 1;
    write y;
};

call fun1;
write y;