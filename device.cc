// Reid Mayfield, cse477, 4/23/2014

#include "aquila.h" // Sound Lib
#include <iostream>
#include <assert.h>
/* Net stuff */
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#define DEVICE_ID 0xdeadbabe
#define HEADER_SIZE 10
#define SAMPLE_RATE_OFF 4
#define BIT_DEPTH_OFF 8
#define CHANNELS_OFF 9

/*
Header Format (Network Order)
+----------------+------------------+---------------+--------------+
| device_id (32) | sample_rate (32) | bit_depth (8) | channels (8) |
+----------------+------------------+---------------+--------------+
*/

// Acts as a client device for prototyping purposes.
// Sends a wav file as PCM samples to the server using TCP
int main(int argc, char **argv) {
  if (argc != 4) {
    std::cout << "Usage: ./device <server_ip> <port> <filename>" << std::endl;
    return EXIT_FAILURE;
  }

  // Connect to server
  struct addrinfo hints, *servinfo, *p;
  int rv, sockfd;
  char s[INET6_ADDRSTRLEN];
  memset(&hints, 0, sizeof(hints));
  hints.ai_family = AF_UNSPEC;
  hints.ai_socktype = SOCK_STREAM;
  if ((rv = getaddrinfo(argv[1], argv[2], &hints, &servinfo)) != 0) {
    fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
    return 1;
  }
  for (p = servinfo; p != NULL; p = p->ai_next) {
    if ((sockfd = socket(p->ai_family, p->ai_socktype, p->ai_protocol)) == -1) {
      perror("client: socket");
      continue;
    }
    if (connect(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
      close(sockfd);
      perror("client: connect");
      continue;
    }
    break;
  }
  if (p == NULL) {
    fprintf(stderr, "client: failed to connect\n");
    return 2;
  }
  freeaddrinfo(servinfo);



  // Reads the metadata using Aquila
  Aquila::WaveFile wav(argv[3]);
  uint32_t sample_rate = htonl(wav.getSampleFrequency());
  uint8_t bit_depth = wav.getBitsPerSample();
  assert(bit_depth == 16 || bit_depth == 8);
  uint8_t channels = wav.getChannelsNum();
  assert(channels == 1);

  // Package as an array
  char header[HEADER_SIZE];
  uint32_t id = htonl(DEVICE_ID);
  memcpy(header, &id, sizeof(id));
  memcpy(&header[SAMPLE_RATE_OFF], &sample_rate, sizeof(sample_rate));
  header[BIT_DEPTH_OFF] = 16;
  header[CHANNELS_OFF] = 1;

  // Convert SampleType (double) array into correct sized PCM
  // NONE OF THIS WORKS BECAUSE SAMPLE TYPE IS A DOUBLE BETWEEN -1 and 1
  //
  //
  //
  // **************************
  //      NOTE
  //
  // The samples must be signed 16 bit values
  //
  // **************************
  int size = wav.getSamplesCount();
    int16_t samples[size];
    for (int i = 0; i < size; i++) {
      if (bit_depth == 8) {
        samples[i] = static_cast<uint16_t>(wav.sample(i) * 127.0);
      } else {
	samples[i] = htole16(static_cast<uint16_t>(wav.sample(i) * 32767.0));
      }
    }

  // Send the data
  if (send(sockfd, &header, HEADER_SIZE, 0) == -1)
    perror("send 1");
  if (send(sockfd, samples, size * sizeof(*samples), 0) == -1) {
    perror("send 2");
  }

  // Close up shop
  close(sockfd);
  return EXIT_SUCCESS;
}
