# /etc/puppet/modules/core2/manifests/params.pp
#class core2::params {

        $core2_version = $::hostname ? {
            default => "1.0.1",
	}
        $core2_base = $::hostname ? {
            default => "/opt",
        }
}
