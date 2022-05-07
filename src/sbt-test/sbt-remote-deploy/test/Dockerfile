FROM hseeberger/scala-sbt:17.0.2_1.6.2_2.13.8

RUN dnf install openssh-server
RUN systemctl start sshd
EXPOSE 22