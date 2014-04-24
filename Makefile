all:
	g++ --std=c++11 -o device device.cc lib/libAquila.a -g -Iinclude/aquila

clean:
	rm -f device
