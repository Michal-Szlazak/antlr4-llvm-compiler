
struct struct1 {
    i32 a;
    i32 b;
    f32 c;
    bool d;
};

struct struct2 {
    i32 a;
    bool b;
};

struct struct1 s1;
s1:a = 10;
s1:b = 10;
s1:c = 10;
s1:d = false;

struct struct2 s2;
s2:a = 20;
s2:b = true;

write "s1:";
write s1:a;
write s1:b;
write s1:c;
write s1:d;
write "";

write "s2:";
write s2:a;
write s2:b;
write "";

write "s1:a + s2:a = ";
write s1:a + s2:a;

write s1:d XOR s2:b;