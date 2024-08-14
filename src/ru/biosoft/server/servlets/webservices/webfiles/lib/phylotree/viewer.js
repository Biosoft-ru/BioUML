var phylotree_extensions = new Object();

$(function() {
      $("#newick_export_modal").on("show.bs.modal", function(e) {
        $('textarea[id$="nwk_export_spec"]').val(
          tree.getNewick(function(node) {
            var tags = [];
            selection_set.forEach(function(d) {
              if (node[d]) {
                tags.push(d);
              }
            });
            if (tags.length) {
              return "{" + tags.join(",") + "}";
            }
            return "";
          })
        );
      });

      $("#newick_file").on("change", function(e) {
        var files = e.target.files; // FileList object

        if (files.length == 1) {
          var f = files[0];
          var reader = new FileReader();

          reader.onload = function(e) {
            var res = e.target.result;
            var warning_div = d3
              .select("#main_display")
              .insert("div", ":first-child");

            tree = new phylotree.phylotree(res);
            global_tree = tree;

            if (!tree["json"]) {
              warning_div
                .attr("class", "alert alert-danger alert-dismissable")
                .html(
                  "<strong>Newick parser error for file " +
                    f.name +
                    ": </strong> In file " +
                    res["error"]
                );
            } else {
              tree.render({
                container: "#tree_container",
                "draw-size-bubbles": false,
                "left-right-spacing": "fixed-step",
                "node-styler": node_colorizer,
                "edge-styler": edge_colorizer
              });

              tree.display.selectionLabel(current_selection_name);

              tree.display.countHandler(count => {
                $("#selected_branch_counter").text(function(d) {
                  return count[current_selection_name];
                });
              });

              // Get selection set names from parsed newick
              if (tree.parsed_tags.length) {
                selection_set = tree.parsed_tags;
              }

              update_selection_names();

              $("#newick_modal").modal("hide");

              $(tree.display.container).empty();
              $(tree.display.container).html(tree.display.show());
            }
          };

          $("#newick-dropdown").dropdown("toggle");
          reader.readAsText(f);
        }
      });

      $("#display_tree").on("click", function(e) {
        tree.options({ branches: "straight" }, true);
      });

      $("#mp_label").on("click", function(e) {
        tree.maxParsimony(true, "Foreground");
      });

      $("[data-direction]").on("click", function(e) {
        var which_function =
          $(this).data("direction") == "vertical"
            ? tree.display.spacing_x.bind(tree.display)
            : tree.display.spacing_y.bind(tree.display);
        which_function(which_function() + +$(this).data("amount")).update();
      });

      $(".phylotree-layout-mode").on("click", function(e) {
        if (tree.display.radial() != ($(this).data("mode") == "radial")) {
          $(".phylotree-layout-mode").toggleClass("active");
          tree.display.radial(!tree.display.radial()).update();
        }
      });

      $("#toggle_animation").on("click", function(e) {
        var current_mode = $(this).hasClass("active");
        $(this).toggleClass("active");
        tree.options({ transitions: !current_mode });
      });

      $(".phylotree-align-toggler").on("click", function(e) {
        var button_align = $(this).data("align");
        var tree_align = tree.display.options.alignTips;

        if (tree_align != button_align) {
          tree.display.alignTips(button_align == "right");
          $(".phylotree-align-toggler").toggleClass("active");
          tree.display.update();
        }
      });

      $("#sort_original").on("click", function(e) {
        tree.resortChildren(function(a, b) {
          return a["original_child_order"] - b["original_child_order"];
        });
      });

      $("#sort_ascending").on("click", function(e) {
        sort_nodes(true);
        tree.display.update();
      });

      $("#sort_descending").on("click", function(e) {
        sort_nodes(false);
        tree.display.update();
      });

      $("#and_label").on("click", function(e) {
        tree.display.internalLabel(function(d) {
          return d.reduce(function(prev, curr) {
            return curr[current_selection_name] && prev;
          }, true);
        }, true);
      });

      $("#or_label").on("click", function(e) {
        tree.display.internalLabel(function(d) {
          return d.reduce(function(prev, curr) {
            return curr[current_selection_name] || prev;
          }, false);
        }, true);
      });

      $("#filter_add").on("click", function(e) {
        tree.display
          .modifySelection(function(d) {
            return d.tag || d[current_selection_name];
          })
          .modifySelection(
            function(d) {
              return false;
            },
            "tag",
            false,
            false
          );
      });

      $("#filter_remove").on("click", function(e) {
        tree.display.modifySelection(function(d) {
          return !d.tag;
        });
      });

      $("#select_all").on("click", function(e) {
        tree.display.modifySelection(function(d) {
          return true;
        });
      });

      $("#select_all_internal").on("click", function(e) {
        tree.display.modifySelection(function(d) {
          return !tree.isLeafNode(d.target);
        });
      });

      $("#select_all_leaves").on("click", function(e) {
        tree.display.modifySelection(function(d) {
          return tree.isLeafNode(d.target);
        });
      });

      $("#select_none").on("click", function(e) {
        tree.display.modifySelection(function(d) {
          return false;
        });
      });

      $("#clear_internal").on("click", function(e) {
        tree.display.modifySelection(function(d) {
          return tree.isLeafNode(d.target)
            ? d.target[current_selection_name]
            : false;
        });
      });

      $("#clear_leaves").on("click", function(e) {
        tree.display.modifySelection(function(d) {
          return !tree.isLeafNode(d.target)
            ? d.target[current_selection_name]
            : false;
        });
      });

      $("#display_dengrogram").on("click", function(e) {
        tree.display.options({ branches: "step" }, true);
      });

      $("#branch_filter").on("input propertychange", function(e) {
        var filter_value = $(this).val();

        var rx = new RegExp(filter_value, "i");

        tree.display.modifySelection(n => {
          if (!n.target.data.name) {
            return false;
          }
          m = n.target.data.name.search(rx);
          return filter_value.length && m != -1;
        }, "tag");
      });

      $("#validate_newick").on("click", function(e) {
        let test_string = $('textarea[id$="nwk_spec"]').val();

        tree = new phylotree.phylotree(test_string);
        global_tree = tree;

        if (!tree["json"]) {
          var warning_div = d3
            .select("#newick_body")
            .selectAll("div  .alert-danger")
            .data([res["error"]]);
          warning_div.enter().append("div");
          warning_div
            .html(function(d) {
              return d;
            })
            .attr("class", "alert-danger");
        } else {
          tree.render({
            container: "#tree_container",
            "draw-size-bubbles": false,
            "node-styler": node_colorizer,
            zoom: false,
            "edge-styler": edge_colorizer
          });

          tree.display.selectionLabel(current_selection_name);

          tree.display.countHandler(count => {
            $("#selected_branch_counter").text(function(d) {
              return count[current_selection_name];
            });
          });

          // Get selection set names from parsed newick
          if (tree.parsed_tags.length) {
            selection_set = tree.parsed_tags;
          }

          update_selection_names();

          $("#newick_modal").modal("hide");
          $(tree.display.container).html(tree.display.show());
        }
      });
      
      var valid_id = new RegExp("^[\\w]+$");

      $("#selection_name_box").on("input propertychange", function(e) {
        var name = $(this).val();

        var accept_name =
          selection_set.indexOf(name) < 0 && valid_id.exec(name);

        d3.select("#save_selection_button").classed(
          "disabled",
          accept_name ? null : true
        );
      });

      $("#selection_rename").on("click", function(e) {
        d3.select("#save_selection_button")
          .classed("disabled", true)
          .on("click", function(e) {
            // save selection handler
            var old_selection_name = current_selection_name;
            selection_set[current_selection_id] = current_selection_name = $(
              "#selection_name_box"
            ).val();

            if (old_selection_name != current_selection_name) {
              tree.update_key_name(old_selection_name, current_selection_name);
              update_selection_names(current_selection_id);
            }
            send_click_event_to_menu_objects(
              new CustomEvent(selection_menu_element_action, {
                detail: ["save", this]
              })
            );
          });

        d3.select("#cancel_selection_button")
          .classed("disabled", false)
          .on("click", function(e) {
            // save selection handler
            $("#selection_name_box").val(current_selection_name);
            send_click_event_to_menu_objects(
              new CustomEvent(selection_menu_element_action, {
                detail: ["cancel", this]
              })
            );
          });

        send_click_event_to_menu_objects(
          new CustomEvent(selection_menu_element_action, {
            detail: ["rename", this]
          })
        );
        e.preventDefault();
      });

      $("#selection_delete").on("click", function(e) {
        tree.display.updateKeyName(selection_set[current_selection_id], null);
        selection_set.splice(current_selection_id, 1);

        if (current_selection_id > 0) {
          current_selection_id--;
        }
        current_selection_name = selection_set[current_selection_id];
        update_selection_names(current_selection_id);
        $("#selection_name_box").val(current_selection_name);

        send_click_event_to_menu_objects(
          new CustomEvent(selection_menu_element_action, {
            detail: ["save", this]
          })
        );
        e.preventDefault();
      });

      $("#selection_new").on("click", function(e) {
        d3.select("#save_selection_button")
          .classed("disabled", true)
          .on("click", function(e) {
            // save selection handler
            current_selection_name = $("#selection_name_box").val();
            current_selection_id = selection_set.length;
            selection_set.push(current_selection_name);
            update_selection_names(current_selection_id);
            send_click_event_to_menu_objects(
              new CustomEvent(selection_menu_element_action, {
                detail: ["save", this]
              })
            );
          });

        d3.select("#cancel_selection_button")
          .classed("disabled", false)
          .on("click", function(e) {
            // save selection handler
            $("#selection_name_box").val(current_selection_name);
            send_click_event_to_menu_objects(
              new CustomEvent(selection_menu_element_action, {
                detail: ["cancel", this]
              })
            );
          });

        send_click_event_to_menu_objects(
          new CustomEvent(selection_menu_element_action, {
            detail: ["new", this]
          })
        );
        e.preventDefault();
      });
  });
      
      
      function sort_nodes(asc) {
          tree.resortChildren(function(a, b) {
            return (b.height - a.height || b.value - a.value) * (asc ? 1 : -1);
          });
        }

      function default_tree_settings() {
        tree = phylotree();
        tree.branchLength(null);
        tree.branchName(null);
        tree.display.radial(false).separation(function(a, b) {
          return 0;
        });
      }

      function node_colorizer(element, data) {
        try {
          var count_class = 0;

          selection_set.forEach(function(d, i) {
            if (data[d]) {
              count_class++;
              element.style(
                "fill",
                color_scheme(i),
                i == current_selection_id ? "important" : null
              );
            }
          });

          if (count_class > 1) {
          } else {
            if (count_class == 0) {
              element.style("fill", null);
            }
          }
        } catch (e) {}
      }

      function edge_colorizer(element, data) {

        try {
          var count_class = 0;

          selection_set.forEach(function(d, i) {
            if (data[d]) {
              count_class++;
              element.style(
                "stroke",
                color_scheme(i),
                i == current_selection_id ? "important" : null
              );
            }
          });

          if (count_class > 1) {
            element.classed("branch-multiple", true);
          } else if (count_class == 0) {
            element.style("stroke", null).classed("branch-multiple", false);
          }
        } catch (e) {}
      }

      
      function send_click_event_to_menu_objects(e) {
        $(
          "#selection_new, #selection_delete, #selection_rename, #save_selection_name, #selection_name_box, #selection_name_dropdown"
        )
          .get()
          .forEach(function(d) {
            d.dispatchEvent(e);
          });
      }

      function update_selection_names(id, skip_rebuild) {
        skip_rebuild = skip_rebuild || false;
        id = id || 0;

        current_selection_name = selection_set[id];
        current_selection_id = id;

        if (!skip_rebuild) {
          d3.selectAll(".selection_set").remove();

          d3.select("#selection_name_dropdown")
            .selectAll(".selection_set")
            .data(selection_set)
            .enter()
            .append("a")
            .attr("class", "selection_set dropdown-item")
            .attr("href", "#")
            .text(function(d) {
              return d;
            })
            .style("color", function(d, i) {
              return color_scheme(i);
            })
            .on("click", function(d, i) {
              update_selection_names(i, true);
            });
        }

        d3.select("#selection_name_box")
          .style("color", color_scheme(id))
          .property("value", current_selection_name);

        // Loop through all selection_sets
        _.each(selection_set, function(id) {
          tree.display.selectionLabel(id);
          tree.display.update();
        });

        tree.display.selectionLabel(selection_set[id]);
        tree.display.update();
      }

      var width = 800, //$(container_id).width(),
        height = 800, //$(container_id).height()
        selection_set = ["Foreground"],
        current_selection_name = $("#selection_name_box").val(),
        current_selection_id = 0,
        max_selections = 10;
      (color_scheme = d3.scaleOrdinal(d3.schemeCategory10)),
        (selection_menu_element_action = "phylotree_menu_element_action");

      var container_id = "#tree_container";

      //var tree = phylotree.phylotree(test_string);
      //.size([height, width]);

      //window.setInterval (function () {});

      var example_controls = d3.select("#controls_form").append("form");

      //var svg = d3.select(container_id).append("svg")
      //    .attr("width", width)
      //    .attr("height", height);

      function selection_handler_name_box(e) {
        var name_box = d3.select(this);
        switch (e.detail[0]) {
          case "save":
          case "cancel":
            name_box
              .property("disabled", true)
              .style("color", color_scheme(current_selection_id));

            break;
          case "new":
            name_box
              .property("disabled", false)
              .property("value", "new_selection_name")
              .style("color", color_scheme(selection_set.length));
            break;
          case "rename":
            name_box.property("disabled", false);
            break;
        }
      }

      function selection_handler_new(e) {
        var element = d3.select(this);
        $(this).data("tooltip", false);
        switch (e.detail[0]) {
          case "save":
          case "cancel":
            if (selection_set.length == max_selections) {
              element.classed("disabled", true);
              $(this).tooltip({
                title: "Up to " + max_selections + " are allowed",
                placement: "left"
              });
            } else {
              element.classed("disabled", null);
            }
            break;
          default:
            element.classed("disabled", true);
            break;
        }
      }

      function selection_handler_rename(e) {
        var element = d3.select(this);
        element.classed(
          "disabled",
          e.detail[0] == "save" || e.detail[0] == "cancel" ? null : true
        );
      }

      function selection_handler_save_selection_name(e) {
        var element = d3.select(this);
        element.style(
          "display",
          e.detail[0] == "save" || e.detail[0] == "cancel" ? "none" : null
        );
      }

      function selection_handler_name_dropdown(e) {
        var element = d3.select(this).selectAll(".selection_set");
        element.classed(
          "disabled",
          e.detail[0] == "save" || e.detail[0] == "cancel" ? null : true
        );
      }

      function selection_handler_delete(e) {
        var element = d3.select(this);
        $(this).tooltip("dispose");
        switch (e.detail[0]) {
          case "save":
          case "cancel":
            if (selection_set.length == 1) {
              element.classed("disabled", true);
              $(this).tooltip({
                title:
                  "At least one named selection set <br> is required;<br>it can be empty, however",
                placement: "bottom",
                html: true
              });
            } else {
              element.classed("disabled", null);
            }
            break;
          default:
            element.classed("disabled", true);
            break;
        }
      }

      var datamonkey_save_image = function(type, container) {
        var prefix = {
          xmlns: "http://www.w3.org/2000/xmlns/",
          xlink: "http://www.w3.org/1999/xlink",
          svg: "http://www.w3.org/2000/svg"
        };

        function get_styles(doc) {
          function process_stylesheet(ss) {
            try {
              if (ss.cssRules) {
                for (var i = 0; i < ss.cssRules.length; i++) {
                  var rule = ss.cssRules[i];
                  if (rule.type === 3) {
                    // Import Rule
                    process_stylesheet(rule.styleSheet);
                  } else {
                    // hack for illustrator crashing on descendent selectors
                    if (rule.selectorText) {
                      if (rule.selectorText.indexOf(">") === -1) {
                        styles += "\n" + rule.cssText;
                      }
                    }
                  }
                }
              }
            } catch (e) {
              console.log("Could not process stylesheet : " + ss); // eslint-disable-line
            }
          }

          var styles = "",
            styleSheets = doc.styleSheets;

          if (styleSheets) {
            for (var i = 0; i < styleSheets.length; i++) {
              process_stylesheet(styleSheets[i]);
            }
          }

          return styles;
        }

        var svg = $(container).find("svg")[0];
        if (!svg) {
          svg = $(container)[0];
        }

        var styles = get_styles(window.document);

        svg.setAttribute("version", "1.1");

        var defsEl = document.createElement("defs");
        svg.insertBefore(defsEl, svg.firstChild);

        var styleEl = document.createElement("style");
        defsEl.appendChild(styleEl);
        styleEl.setAttribute("type", "text/css");

        // removing attributes so they aren't doubled up
        svg.removeAttribute("xmlns");
        svg.removeAttribute("xlink");

        // These are needed for the svg
        if (!svg.hasAttributeNS(prefix.xmlns, "xmlns")) {
          svg.setAttributeNS(prefix.xmlns, "xmlns", prefix.svg);
        }

        if (!svg.hasAttributeNS(prefix.xmlns, "xmlns:xlink")) {
          svg.setAttributeNS(prefix.xmlns, "xmlns:xlink", prefix.xlink);
        }

        var source = new XMLSerializer()
          .serializeToString(svg)
          .replace("</style>", "<![CDATA[" + styles + "]]></style>");
        var doctype =
          '<?xml version="1.0" standalone="no"?><!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">';
        var to_download = [doctype + source];
        var image_string =
          "data:image/svg+xml;base66," + encodeURIComponent(to_download);

        if (navigator.msSaveBlob) {
          // IE10
          download(image_string, "image.svg", "image/svg+xml");
        } else if (type == "png") {
          b64toBlob(
            image_string,
            function(blob) {
              var url = window.URL.createObjectURL(blob);
              var pom = document.createElement("a");
              pom.setAttribute("download", "image.png");
              pom.setAttribute("href", url);
              $("body").append(pom);
              pom.click();
              pom.remove();
            },
            function(error) {
              console.log(error); // eslint-disable-line
            }
          );
        } else {
          var pom = document.createElement("a");
          pom.setAttribute("download", "image.svg");
          pom.setAttribute("href", image_string);
          $("body").append(pom);
          pom.click();
          pom.remove();
        }
      };

      $(document).ready(function() {
        var loc = window.location.toString();
        var params = loc.split('?')[1].split('&');
        var deName = params[0];
        var title = params[1];
        var viewer;
        window.parent.queryBioUML("web/doc/getcontent", {"de":decodeURIComponent(deName)}, function(data)
            {
                tree = new phylotree.phylotree(data.values);
                global_tree = tree;

                tree.render({
                  container: "#tree_container",
                  "draw-size-bubbles": false,
                  "node-styler": node_colorizer,
                  zoom: false,
                  "edge-styler": edge_colorizer
                });
                
                $('#tree_container').on('reroot', function (e) {  
          update_selection_names();

          tree.display.countHandler(count => {
            $("#selected_branch_counter").text(function(d) {
              return count[current_selection_name];
            });
          });

        });

        tree.display.selectionLabel(current_selection_name);

        tree.display.countHandler(count => {
          $("#selected_branch_counter").text(function(d) {
            return count[current_selection_name];
          });
        });

        // Get selection set names from parsed newick
        if (tree.parsed_tags.length) {
          selection_set = tree.parsed_tags;
        }

        // Until a cleaner solution to supporting both Observable and regular HTML
        $(tree.display.container).append(tree.display.show());

        $("#selection_new")
          .get(0)
          .addEventListener(
            selection_menu_element_action,
            selection_handler_new,
            false
          );
        $("#selection_rename")
          .get(0)
          .addEventListener(
            selection_menu_element_action,
            selection_handler_rename,
            false
          );
        $("#selection_delete")
          .get(0)
          .addEventListener(
            selection_menu_element_action,
            selection_handler_delete,
            false
          );
        $("#selection_delete")
          .get(0)
          .dispatchEvent(
            new CustomEvent(selection_menu_element_action, {
              detail: ["cancel", null]
            })
          );
        $("#selection_name_box")
          .get(0)
          .addEventListener(
            selection_menu_element_action,
            selection_handler_name_box,
            false
          );
        $("#save_selection_name")
          .get(0)
          .addEventListener(
            selection_menu_element_action,
            selection_handler_save_selection_name,
            false
          );
        $("#selection_name_dropdown")
          .get(0)
          .addEventListener(
            selection_menu_element_action,
            selection_handler_name_dropdown,
            false
          );

        update_selection_names();

        $("#save_image").on("click", function(e) {
          datamonkey_save_image("svg", "#tree_container");
        });
        });
        

        
      });

  var _warn = console.warn;

  console.warn = function() {
    if (arguments[0].includes("Phylotree User Warning")) {
      var warning_div = d3
        .select("#main_display")
        .insert("div", ":first-child");
      warning_div
        .attr("class", "alert alert-danger alert-dismissable")
        .html(arguments[0]);
    }
    return _warn.apply(console, arguments);
  };
