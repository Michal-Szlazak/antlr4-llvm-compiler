
loop(2) {
    write "loop(2)";
};

i32 x;
x = 3;

loop(x) {
    write "loop(x) x = 3";
};

bool x;
x = true;

loop(false) {
    write "loop(false)";
};