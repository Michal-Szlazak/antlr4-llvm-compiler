
struct habibi {
    i32 x;
    bool y;
};

struct babe {
    i32 a;
};

struct babe b;
b:a = 2;

fun fun1 {

   struct habibi h;
   h:x = 2;
   write "Struct habibi x:";
   write h:x;
   write b:a;
};

fun fun3 {

};


call fun1;
call fun3;