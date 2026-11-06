#!/usr/bin/env perl
# Converts CC&A's 1.21.1 NeoForge data (src/generated/resources/data, src/main/resources/data) to 26.1 / Create Fly
# formats. Datagen is stubbed, so this stands in for the recipe provider. Adapted from D&D's converter.
#
# Run once from the repo root, on the unconverted data: perl porting-tools/convert_data_26.pl (not safe to rerun)
#
# - recipes: ingredient objects ({"item"}/{"tag"}) -> "id" / "#tag"; Create processing recipes to Create Fly's codecs:
#   "ingredients": [x] -> "ingredient": x for crushing/pressing/filling/rolling/charging; mixing/compacting fluid
#   ingredients/results split out into fluid_ingredients/fluid_results (amounts mB -> droplets, x81);
#   mechanical_crafting like shaped. CC&A's own liquid_burning gets a fluid_ingredient (see PORTING-26.1.md).
# - neoforge:conditions -> fabric:load_conditions (mod_loaded -> fabric:all_mods_loaded, not+tag_empty and
#   createaddition:has_fluid_tag -> fabric:tags_populated).
# - compat recipes (data/createaddition/recipe/compat) for mods without 26.1 Fabric builds or in foreign recipe types
#   (Mekanism, Immersive Engineering, Thermal, Tinkers, JEED) are deleted; AE2's is kept.
# - advancements: neoforge:conditions -> fabric:load_conditions.
use strict;
use warnings;
use File::Find;
use File::Path qw(remove_tree);
use JSON::PP;

my $json = JSON::PP->new->utf8->canonical->pretty->indent_length(2)->space_before(0);
my @roots = ('src/generated/resources/data', 'src/main/resources/data');
my %warned;
my %count;

sub warn_once { my $m = shift; print STDERR "warning: $m\n" unless $warned{$m}++; }
sub read_json { my $f = shift; open my $in, '<:raw', $f or die "$f: $!"; local $/; my $d = $json->decode(<$in>); close $in; return $d; }
sub write_json { my ($f, $d) = @_; open my $out, '>:raw', $f or die "$f: $!"; print $out $json->encode($d); close $out; }
sub files_under { my $dir = shift; my @f; find(sub { push @f, $File::Find::name if /\.json$/ }, $dir) if -d $dir; return sort @f; }

sub ingredient {
    my $in = shift;
    return $in unless ref $in;
    if (ref $in eq 'ARRAY') {
        my @alts = map { ingredient($_) } @$in;
        return $alts[0] if @alts == 1;
        return \@alts unless grep { /^#/ } @alts;
        warn_once("alternative tags reduced to the first: @alts");
        return $alts[0];
    }
    return $in->{item} if exists $in->{item};
    return "#$in->{tag}" if exists $in->{tag};
    die 'unknown ingredient ' . $json->encode($in);
}

sub is_fluid_ing { my $x = shift; return ref $x eq 'HASH' && ($x->{type} // '') =~ /^(neoforge:(tag|single)|fluid_stack|fluid)$/; }
sub is_fluid_result { my $x = shift; return ref $x eq 'HASH' && exists $x->{amount}; }

sub fluid_ingredient {
    my $f = shift;
    my $amount = $f->{amount} * 81;
    if (exists $f->{tag}) { return { type => 'fluid_tag', amount => $amount, fluid_tag => "#$f->{tag}" }; }
    if (exists $f->{fluid_tag}) { return { type => 'fluid_tag', amount => $amount, fluid_tag => "#$f->{fluid_tag}" }; }
    return { type => 'fluid_stack', amount => $amount, fluid => ($f->{fluid} // $f->{id}) };
}

sub fluid_result { my $f = shift; return { id => $f->{id} // $f->{fluid}, amount => $f->{amount} * 81 }; }

sub output {
    my $r = shift;
    return { id => $r } unless ref $r;
    my %out = (id => $r->{item} // $r->{id});
    $out{count} = $r->{count} if ($r->{count} // 1) != 1;
    $out{chance} = $r->{chance} if exists $r->{chance} && $r->{chance} != 1;
    $out{components} = $r->{components} if exists $r->{components};
    return \%out;
}

sub convert_condition {
    my $c = shift;
    my $t = $c->{type};
    return { condition => 'fabric:all_mods_loaded', values => [ $c->{modid} ] } if $t eq 'neoforge:mod_loaded';
    if ($t eq 'neoforge:not' && $c->{value}{type} eq 'neoforge:tag_empty') {
        return { condition => 'fabric:tags_populated', registry => 'minecraft:item', values => [ $c->{value}{tag} ] };
    }
    return { condition => 'fabric:tags_populated', registry => 'minecraft:fluid', values => [ $c->{fluidTag} ] }
        if $t eq 'createaddition:has_fluid_tag';
    die "unhandled condition $t";
}

sub load_conditions {
    my $cs = shift;
    my @c = map { convert_condition($_) } @$cs;
    return $c[0] if @c == 1;
    return { condition => 'fabric:and', values => \@c };
}

sub convert_recipe {
    my $r = shift;
    my $type = $r->{type};
    my %out = (type => $type);
    $out{'fabric:load_conditions'} = load_conditions($r->{'neoforge:conditions'}) if $r->{'neoforge:conditions'};
    for my $k (qw(category group show_notification)) { $out{$k} = $r->{$k} if exists $r->{$k}; }
    my @ing = ref $r->{ingredients} eq 'ARRAY' ? @{ $r->{ingredients} } : ();

    if ($type eq 'minecraft:crafting_shaped' || $type eq 'create:mechanical_crafting') {
        $out{pattern} = $r->{pattern};
        $out{key} = { map { $_ => ingredient($r->{key}{$_}) } keys %{ $r->{key} } };
        $out{accept_mirrored} = $r->{accept_mirrored} if exists $r->{accept_mirrored};
        $out{result} = output($r->{result});
    } elsif ($type eq 'minecraft:crafting_shapeless') {
        $out{ingredients} = [ map { ingredient($_) } @ing ];
        $out{result} = output($r->{result});
    } elsif ($type =~ /^minecraft:(smelting|blasting|smoking|campfire_cooking)$/) {
        $out{ingredient} = ingredient($r->{ingredient});
        $out{result} = output($r->{result});
        $out{experience} = $r->{experience} if exists $r->{experience};
        $out{cookingtime} = $r->{cookingtime} if exists $r->{cookingtime};
    } elsif ($type eq 'create:mixing' || $type eq 'create:compacting') {
        my @items = grep { !is_fluid_ing($_) } @ing;
        my @fluids = grep { is_fluid_ing($_) } @ing;
        my @res = @{ $r->{results} // [] };
        my @item_results = grep { !is_fluid_result($_) } @res;
        my @fluid_results = grep { is_fluid_result($_) } @res;
        $out{ingredients} = [ map { ingredient($_) } @items ] if @items;
        $out{fluid_ingredients} = [ map { fluid_ingredient($_) } @fluids ] if @fluids;
        $out{results} = [ map { output($_) } @item_results ] if @item_results;
        if (@fluid_results) {
            if ($type eq 'create:mixing') {
                $out{fluid_results} = [ map { fluid_result($_) } @fluid_results ];
            } else {
                warn_once("Create Fly compacting recipes have no fluid results; dropped them");
                $count{'compacting fluid results dropped'}++;
            }
        }
        $out{heat_requirement} = $r->{heat_requirement} if exists $r->{heat_requirement};
        $out{processing_time} = $r->{processing_time} if exists $r->{processing_time};
    } elsif ($type eq 'create:filling') {
        die 'filling shape' unless @ing == 2;
        $out{ingredient} = ingredient($ing[0]);
        $out{fluid_ingredient} = fluid_ingredient($ing[1]);
        $out{result} = output($r->{results}[0]);
    } elsif ($type eq 'create:crushing') {
        $out{ingredient} = ingredient($ing[0]);
        $out{processing_time} = $r->{processing_time} // 100;
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
    } elsif ($type eq 'create:pressing') {
        $out{ingredient} = ingredient($ing[0]);
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
    } elsif ($type eq 'createaddition:rolling') {
        $out{ingredient} = ingredient($ing[0]);
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
    } elsif ($type eq 'createaddition:charging') {
        $out{ingredient} = ingredient($ing[0]);
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
        $out{energy} = $r->{energy};
        $out{max_charge_rate} = $r->{max_charge_rate};
    } elsif ($type eq 'createaddition:liquid_burning') {
        $out{fluid_ingredient} = fluid_ingredient($ing[0]);
        $out{burn_time} = $r->{burn_time};
        $out{superheated} = $r->{superheated} if $r->{superheated};
    } else {
        die "unsupported recipe type $type";
    }
    return \%out;
}

for my $data (@roots) {
    my $compat = "$data/createaddition/recipe/compat";
    if (-d $compat) {
        for my $d (glob "$compat/*") {
            next if $d =~ m{/ae2$};
            $count{'compat files removed'} += scalar(() = (glob("$d/*"), glob("$d/*/*")));
            remove_tree($d);
        }
    }
    for my $f (files_under("$data/createaddition/recipe"), files_under("$data/create/recipe")) {
        my $c = convert_recipe(read_json($f));
        if ($c->{type} eq q(create:compacting) && !$c->{results}) { unlink $f; $count{q(fluid-only compacting recipes removed)}++; next; }
        write_json($f, $c);
        $count{'recipes converted'}++;
    }
    for my $f (files_under("$data/createaddition/advancement")) {
        my $d = read_json($f);
        next unless $d->{'neoforge:conditions'};
        $d->{'fabric:load_conditions'} = load_conditions(delete $d->{'neoforge:conditions'});
        write_json($f, $d);
        $count{'advancements converted'}++;
    }
}
print join(', ', map { "$count{$_} $_" } sort keys %count) . "\n";
