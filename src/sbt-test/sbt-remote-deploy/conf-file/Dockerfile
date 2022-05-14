FROM debian:latest

RUN apt-get update && echo y | apt-get install openssh-server openjdk-11-jre curl

RUN echo "root:example" | chpasswd
RUN rm /etc/ssh/sshd_config
COPY sshd_config /etc/ssh/sshd_config
EXPOSE 22

RUN curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz | gzip -d > cs
RUN chmod +x cs
RUN ./cs setup --yes --apps scala:2.13.8
ENV PATH="/root/.local/share/coursier/bin:${PATH}"

CMD service ssh start && while true; do sleep 10; done;
