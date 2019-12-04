FROM alpine:latest

ENV SCALA_VERSION=2.12.8 \
  SBT_VERSION=1.2.8 \
  SCALA_HOME=/usr/share/scala

RUN apk add git \
  && apk add openssh \
  && apk add openrc \
  && apk add openjdk8-jre \
  # Scala related stuff
  && echo "$SCALA_VERSION $SBT_VERSION" \
  && mkdir -p /usr/lib/jvm/java-1.8-openjdk/jre \
  && touch /usr/lib/jvm/java-1.8-openjdk/jre/release \
  && apk add --no-cache bash \
  && apk add --no-cache curl \
  && curl -fsL http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /usr/local \
  && ln -s /usr/local/scala-$SCALA_VERSION/bin/* /usr/local/bin/ \
  && scala -version \
  && scalac -version \
  && curl -fsL https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz | tar xfz - -C /usr/local \
  && $(mv /usr/local/sbt-launcher-packaging-$SBT_VERSION /usr/local/sbt || true) \
  && ln -s /usr/local/sbt/bin/* /usr/local/bin/ \
  && sbt -mem 256 sbt-version || sbt -mem 256 sbtVersion || true \
  # end of Scala related stuff
  && rc-update add sshd \
  && sed -i s/#PermitRootLogin.*/PermitRootLogin\ yes/ /etc/ssh/sshd_config \
  && echo "root:root" | chpasswd \
  && rm -rf /var/cache/apk/* \
  && sed -ie 's/#Port 22/Port 22/g' /etc/ssh/sshd_config \
  && sed -ri 's/#HostKey \/etc\/ssh\/ssh_host_key/HostKey \/etc\/ssh\/ssh_host_key/g' /etc/ssh/sshd_config \
  && sed -ir 's/#HostKey \/etc\/ssh\/ssh_host_rsa_key/HostKey \/etc\/ssh\/ssh_host_rsa_key/g' /etc/ssh/sshd_config \
  && sed -ir 's/#HostKey \/etc\/ssh\/ssh_host_dsa_key/HostKey \/etc\/ssh\/ssh_host_dsa_key/g' /etc/ssh/sshd_config \
  && sed -ir 's/#HostKey \/etc\/ssh\/ssh_host_ecdsa_key/HostKey \/etc\/ssh\/ssh_host_ecdsa_key/g' /etc/ssh/sshd_config \
  && sed -ir 's/#HostKey \/etc\/ssh\/ssh_host_ed25519_key/HostKey \/etc\/ssh\/ssh_host_ed25519_key/g' /etc/ssh/sshd_config \
  && /usr/bin/ssh-keygen -A \
  && ssh-keygen -t rsa -b 4096 -f  /etc/ssh/ssh_host_key

EXPOSE 22/tcp
CMD ["/usr/sbin/sshd","-D"]
