package ParameterFiles;
use File::Spec;
use File::Basename;

sub trim {
    (my $s) = @_;
    $s =~ s/^\s+//;
    $s =~ s/\s+$//;
    return $s;
}

sub expand {
    (my $s, $params) = @_;
    foreach my $key (keys %$params) {
        my $val = $params->{$key};
        $s =~ s/%$key%/$val/;
    }
    return $s;
}

sub parse {
    my $v = shift;
    if (lc($v) eq 'true') {
        return 1;
    } elsif (lc($v) eq 'false') {
        return 0;
    } else {
        return $v;
    }
}

sub load_param_file {
    (my $param_file_name) = @_;
    my %params;
    load_param_file_internal($param_file_name, \%params);
    print "Loaded Params are:\n";
    foreach my $key (sort keys %params) {
        printf("%45s:\t%s\n", $key, $params{$key});
    }
    return \%params;
}

sub load_param_file_internal {
    (my $param_file_name, $params) = @_;
    print "Loading parameter file $param_file_name\n";
    open(my $inp, $param_file_name) or die "Failed to open parameter file.\n";

    my $line;
    while ($line = <$inp>) {
        my $override = 0;
        chomp($line);
        if ($line and not $line =~ /^#/) { # skip comments
            if ($line =~ /^@(.*)/ || $line =~ /^INCLUDE (.*)/) {
                load_param_file_internal(File::Spec->rel2abs($1, dirname($param_file_name)), $params);
            } else {
                if ($line =~ /^OVERRIDE (.*)/) {
                    $override = 1;
                    $line = $1;
                }
                my @parts = split /:/, $line, 2;
                if (scalar(@parts) == 2) {
                    my $name = $parts[0];
                    my $val = $parts[1];
                    $name = trim($name);
                    $val = trim($val);
                    $val = expand($val, $params);
                    $val = parse($val);
                    if (exists $params->{$name} and not override) {
                        print "Attempting to override $name without explicit override.\n";
                    } else {
                        $params->{$name} = $val;
                    }
                }
            }
        }
    }

    close(INP);
}


return 1;
