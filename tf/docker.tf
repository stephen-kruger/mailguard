provider "docker" {}

#-----------------------------------
# declare any input variables
#-----------------------------------
variable mailguard_http_port {
  default = "8000"
}

variable mailguard_smtp_port {
  default = "25"
}

variable mailguard_hostname {
  default = "mailguard"
}

#-----------------------------------
# create Java mailguard app container
#-----------------------------------
resource "docker_container" "mailguard-container" {
	name  = "mailguard"
	hostname  = "${var.mailguard_hostname}"
	image = "mailguard:latest"
	restart = "always"
	env = [
		"HOSTNAME=${var.mailguard_hostname}"
	]
	ports {
		internal = "80"
		external = "${var.mailguard_http_port}"
	}
	ports {
		internal = "${var.mailguard_smtp_port}"
		external = "25"
	}
}